import express from 'express';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import { Pool } from 'pg';
import { OAuth2Client } from 'google-auth-library';
import { SignJWT, jwtVerify } from 'jose';
import { randomUUID } from 'node:crypto';
import { generateRefreshToken, hashRefreshToken, isValidSigningSecretHex } from './security.js';

const required = ['DATABASE_URL', 'GOOGLE_OIDC_CLIENT_ID', 'ACCESS_TOKEN_HMAC_SECRET_HEX', 'ACCESS_TOKEN_ISSUER', 'ACCESS_TOKEN_AUDIENCE'];
for (const name of required) if (!process.env[name]) throw new Error(`Missing required environment variable: ${name}`);
const secretHex = process.env.ACCESS_TOKEN_HMAC_SECRET_HEX;
if (!isValidSigningSecretHex(secretHex)) throw new Error('ACCESS_TOKEN_HMAC_SECRET_HEX must be exactly 32 random bytes encoded as 64 hex characters.');

const pool = new Pool({ connectionString: process.env.DATABASE_URL, max: 10, ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: true } : undefined });
const googleClient = new OAuth2Client(process.env.GOOGLE_OIDC_CLIENT_ID);
const signingKey = Buffer.from(secretHex, 'hex');
const issuer = process.env.ACCESS_TOKEN_ISSUER;
const audience = process.env.ACCESS_TOKEN_AUDIENCE;
const accessTtl = Number(process.env.ACCESS_TOKEN_TTL_SECONDS || 600);
const refreshDays = Number(process.env.REFRESH_TOKEN_TTL_DAYS || 30);
if (!Number.isInteger(accessTtl) || accessTtl < 60 || accessTtl > 1800) throw new Error('ACCESS_TOKEN_TTL_SECONDS must be between 60 and 1800.');
if (!Number.isInteger(refreshDays) || refreshDays < 1 || refreshDays > 90) throw new Error('REFRESH_TOKEN_TTL_DAYS must be between 1 and 90.');
if (process.env.NODE_ENV === 'production' && !issuer.startsWith('https://')) throw new Error('ACCESS_TOKEN_ISSUER must use HTTPS in production.');
const app = express();

app.disable('x-powered-by');
app.use(helmet());
app.use(express.json({ limit: '64kb', strict: true }));
app.use('/api/auth', rateLimit({ windowMs: 60_000, limit: 20, standardHeaders: 'draft-8', legacyHeaders: false }));
app.use('/api', rateLimit({ windowMs: 60_000, limit: 120, standardHeaders: 'draft-8', legacyHeaders: false }));

const refreshExpiry = () => new Date(Date.now() + refreshDays * 86400_000);
const jsonError = (res, status, code, message) => res.status(status).json({ error: { code, message } });

async function issueAccessToken(user, sessionId) {
  return new SignJWT({ sid: sessionId, email: user.email })
    .setProtectedHeader({ alg: 'HS256', typ: 'JWT' })
    .setIssuer(issuer).setAudience(audience).setSubject(user.id)
    .setIssuedAt().setExpirationTime(`${accessTtl}s`).sign(signingKey);
}

async function requireSession(req, res, next) {
  try {
    const header = req.get('authorization') || '';
    const match = /^Bearer ([A-Za-z0-9._~-]+)$/.exec(header);
    if (!match) return jsonError(res, 401, 'AUTH_REQUIRED', 'A valid AI I.Z access token is required.');
    const { payload } = await jwtVerify(match[1], signingKey, { issuer, audience, algorithms: ['HS256'] });
    if (typeof payload.sub !== 'string' || typeof payload.sid !== 'string') return jsonError(res, 401, 'INVALID_SESSION', 'The session token is invalid.');
    const result = await pool.query(
      `SELECT u.id, u.email, u.display_name, u.avatar_url, u.status, s.id AS session_id
       FROM app_sessions s JOIN app_users u ON u.id=s.user_id
       WHERE s.id=$1 AND s.user_id=$2 AND s.revoked_at IS NULL AND s.expires_at > now() AND u.status='active'`,
      [payload.sid, payload.sub]
    );
    if (!result.rowCount) return jsonError(res, 401, 'SESSION_REVOKED', 'The session is expired or revoked.');
    req.user = result.rows[0];
    next();
  } catch {
    return jsonError(res, 401, 'INVALID_SESSION', 'The session token is invalid or expired.');
  }
}

app.get('/api/health', async (_req, res) => {
  try { await pool.query('SELECT 1'); res.json({ status: 'ok', service: 'ai-iz-api', database: 'reachable' }); }
  catch { jsonError(res, 503, 'DATABASE_UNAVAILABLE', 'The service database is unavailable.'); }
});

app.post('/api/auth/session', async (req, res) => {
  const idToken = req.body?.idToken;
  if (typeof idToken !== 'string' || idToken.length < 100 || idToken.length > 8192) return jsonError(res, 400, 'INVALID_GOOGLE_TOKEN', 'A Google ID token is required.');
  let claims;
  try {
    const ticket = await googleClient.verifyIdToken({ idToken, audience: process.env.GOOGLE_OIDC_CLIENT_ID });
    claims = ticket.getPayload();
    if (!claims || !claims.sub || !['https://accounts.google.com', 'accounts.google.com'].includes(claims.iss) || claims.email_verified !== true || !claims.email) throw new Error('Invalid OIDC claims');
  } catch {
    return jsonError(res, 401, 'GOOGLE_TOKEN_REJECTED', 'Google could not verify this sign-in. Please try again.');
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    // Serialize first-time provisioning for this Google subject to avoid duplicate-account races.
    await client.query('SELECT pg_advisory_xact_lock(hashtextextended($1, 0))', [claims.sub]);
    const identity = await client.query(
      `SELECT u.* FROM external_identities e JOIN app_users u ON u.id=e.user_id
       WHERE e.provider='google' AND e.provider_subject=$1 FOR UPDATE OF u`, [claims.sub]);
    let user = identity.rows[0];
    if (!user) {
      const created = await client.query(
        `INSERT INTO app_users(display_name,email,email_verified,avatar_url)
         VALUES($1,$2,true,$3) RETURNING *`,
        [claims.name || '', claims.email, claims.picture || null]);
      user = created.rows[0];
      await client.query(
        `INSERT INTO external_identities(user_id,provider,provider_subject) VALUES($1,'google',$2)`,
        [user.id, claims.sub]);
    } else {
      if (user.status !== 'active') throw Object.assign(new Error('Account disabled'), { statusCode: 403 });
      await client.query(
        `UPDATE app_users SET display_name=$2,email=$3,email_verified=true,avatar_url=$4,updated_at=now() WHERE id=$1`,
        [user.id, claims.name || user.display_name, claims.email, claims.picture || user.avatar_url]);
      user.email = claims.email;
      user.display_name = claims.name || user.display_name;
      user.avatar_url = claims.picture || user.avatar_url;
    }
    if (user.status !== 'active') throw Object.assign(new Error('Account disabled'), { statusCode: 403 });
    const sessionId = randomUUID();
    const familyId = randomUUID();
    const refreshToken = generateRefreshToken();
    await client.query(
      `INSERT INTO app_sessions(id,user_id,family_id,refresh_token_hash,expires_at)
       VALUES($1,$2,$3,$4,$5)`,
      [sessionId, user.id, familyId, hashRefreshToken(refreshToken), refreshExpiry()]);
    await client.query(
      `INSERT INTO security_audit_events(user_id,event_type,session_id) VALUES($1,'login', $2)`,
      [user.id, sessionId]);
    await client.query('COMMIT');
    res.status(200).json({
      accessToken: await issueAccessToken(user, sessionId),
      refreshToken,
      expiresIn: accessTtl,
      user: { id: user.id, email: user.email, displayName: user.display_name, avatarUrl: user.avatar_url }
    });
  } catch (error) {
    await client.query('ROLLBACK');
    if (error.statusCode === 403) return jsonError(res, 403, 'ACCOUNT_DISABLED', 'This account is disabled.');
    console.error('Session provisioning failed:', error.message);
    return jsonError(res, 500, 'SESSION_CREATION_FAILED', 'The account session could not be created.');
  } finally { client.release(); }
});

app.post('/api/auth/refresh', async (req, res) => {
  const token = req.body?.refreshToken;
  if (typeof token !== 'string' || token.length < 50 || token.length > 256) return jsonError(res, 400, 'INVALID_REFRESH_TOKEN', 'A valid refresh token is required.');
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const found = await client.query(
      `SELECT s.*,u.id AS uid,u.email,u.display_name,u.avatar_url,u.status
       FROM app_sessions s JOIN app_users u ON u.id=s.user_id
       WHERE s.refresh_token_hash=$1 FOR UPDATE OF s,u`, [hashRefreshToken(token)]);
    const row = found.rows[0];
    if (!row) { await client.query('ROLLBACK'); return jsonError(res, 401, 'REFRESH_REJECTED', 'The refresh token is invalid. Sign in again.'); }
    if (row.revoked_at) {
      await client.query(`UPDATE app_sessions SET revoked_at=COALESCE(revoked_at,now()) WHERE family_id=$1`, [row.family_id]);
      await client.query(`INSERT INTO security_audit_events(user_id,event_type,session_id,details) VALUES($1,'refresh_reuse_detected',$2,'{}')`, [row.uid,row.id]);
      await client.query('COMMIT');
      return jsonError(res, 401, 'REFRESH_REUSE_DETECTED', 'This session family was revoked. Sign in again.');
    }
    if (new Date(row.expires_at).getTime() <= Date.now() || row.status !== 'active') {
      await client.query('UPDATE app_sessions SET revoked_at=now() WHERE id=$1', [row.id]);
      await client.query('COMMIT');
      return jsonError(res, 401, 'REFRESH_EXPIRED', 'The refresh token expired. Sign in again.');
    }
    const nextId = randomUUID();
    const nextToken = generateRefreshToken();
    // Insert the replacement first so the immediate replaced_by foreign key is valid.
    await client.query(
      `INSERT INTO app_sessions(id,user_id,family_id,refresh_token_hash,expires_at)
       VALUES($1,$2,$3,$4,$5)`,
      [nextId,row.uid,row.family_id,hashRefreshToken(nextToken),refreshExpiry()]);
    await client.query('UPDATE app_sessions SET revoked_at=now(),replaced_by=$2,last_used_at=now() WHERE id=$1', [row.id,nextId]);
    await client.query(
      `INSERT INTO security_audit_events(user_id,event_type,session_id) VALUES($1,'refresh',$2)`,
      [row.uid,nextId]);
    await client.query('COMMIT');
    const user = { id: row.uid, email: row.email, display_name: row.display_name, avatar_url: row.avatar_url };
    res.json({ accessToken: await issueAccessToken(user,nextId), refreshToken: nextToken, expiresIn: accessTtl });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Refresh failed:', error.message);
    jsonError(res, 500, 'REFRESH_FAILED', 'The session could not be refreshed.');
  } finally { client.release(); }
});

app.post('/api/auth/logout', requireSession, async (req, res) => {
  await pool.query('UPDATE app_sessions SET revoked_at=now() WHERE id=$1 AND user_id=$2 AND revoked_at IS NULL', [req.user.session_id,req.user.id]);
  await pool.query(`INSERT INTO security_audit_events(user_id,event_type,session_id) VALUES($1,'logout',$2)`, [req.user.id,req.user.session_id]);
  res.status(204).end();
});

app.get('/api/me', requireSession, (req, res) => {
  res.json({ user: { id: req.user.id, email: req.user.email, displayName: req.user.display_name, avatarUrl: req.user.avatar_url } });
});


const allowedCollections = new Set(['settings', 'tasks', 'approvals', 'chat_messages', 'operation_logs']);
const validRecordId = value => typeof value === 'string' && value.length >= 1 && value.length <= 128 && /^[A-Za-z0-9_.:-]+$/.test(value);

app.get('/api/data/:collection', requireSession, async (req, res) => {
  const { collection } = req.params;
  if (!allowedCollections.has(collection)) return jsonError(res, 404, 'COLLECTION_NOT_FOUND', 'The requested collection is not available.');
  const limit = Math.min(Math.max(Number(req.query.limit) || 100, 1), 200);
  const result = await pool.query(
    'SELECT record_id, payload, created_at, updated_at FROM user_records WHERE user_id=$1 AND collection=$2 ORDER BY updated_at DESC LIMIT $3',
    [req.user.id, collection, limit]
  );
  res.json({ records: result.rows.map(row => ({ id: row.record_id, ...row.payload, createdAt: row.created_at, updatedAt: row.updated_at })) });
});

app.get('/api/data/:collection/:recordId', requireSession, async (req, res) => {
  const { collection, recordId } = req.params;
  if (!allowedCollections.has(collection) || !validRecordId(recordId)) return jsonError(res, 404, 'RECORD_NOT_FOUND', 'The requested record does not exist.');
  const result = await pool.query(
    'SELECT record_id, payload, created_at, updated_at FROM user_records WHERE user_id=$1 AND collection=$2 AND record_id=$3',
    [req.user.id, collection, recordId]
  );
  if (!result.rowCount) return jsonError(res, 404, 'RECORD_NOT_FOUND', 'The requested record does not exist.');
  const row = result.rows[0];
  res.json({ record: { id: row.record_id, ...row.payload, createdAt: row.created_at, updatedAt: row.updated_at } });
});

app.put('/api/data/:collection/:recordId', requireSession, async (req, res) => {
  const { collection, recordId } = req.params;
  if (!allowedCollections.has(collection) || !validRecordId(recordId)) return jsonError(res, 400, 'INVALID_RECORD', 'The collection or record identifier is invalid.');
  if (!req.body || typeof req.body !== 'object' || Array.isArray(req.body)) return jsonError(res, 400, 'INVALID_RECORD', 'A JSON object is required.');
  const payload = { ...req.body, userId: req.user.id };
  delete payload.id;
  delete payload.createdAt;
  delete payload.updatedAt;
  const result = await pool.query(
    `INSERT INTO user_records(user_id,collection,record_id,payload)
     VALUES($1,$2,$3,$4::jsonb)
     ON CONFLICT(user_id,collection,record_id) DO UPDATE
       SET payload=EXCLUDED.payload, updated_at=now()
     RETURNING record_id,payload,created_at,updated_at`,
    [req.user.id, collection, recordId, JSON.stringify(payload)]
  );
  const row = result.rows[0];
  await pool.query(
    'INSERT INTO security_audit_events(user_id,event_type,details) VALUES($1,$2,$3::jsonb)',
    [req.user.id, 'data_write', JSON.stringify({ collection, recordId })]
  );
  res.json({ record: { id: row.record_id, ...row.payload, createdAt: row.created_at, updatedAt: row.updated_at } });
});

app.delete('/api/data/:collection/:recordId', requireSession, async (req, res) => {
  const { collection, recordId } = req.params;
  if (!allowedCollections.has(collection) || !validRecordId(recordId)) return jsonError(res, 404, 'RECORD_NOT_FOUND', 'The requested record does not exist.');
  await pool.query('DELETE FROM user_records WHERE user_id=$1 AND collection=$2 AND record_id=$3', [req.user.id, collection, recordId]);
  await pool.query(
    'INSERT INTO security_audit_events(user_id,event_type,details) VALUES($1,$2,$3::jsonb)',
    [req.user.id, 'data_delete', JSON.stringify({ collection, recordId })]
  );
  res.status(204).end();
});

app.use((_req, res) => jsonError(res, 404, 'NOT_FOUND', 'The requested API route does not exist.'));
app.use((error, _req, res, _next) => {
  console.error('Unhandled request error:', error.message);
  jsonError(res, 500, 'INTERNAL_ERROR', 'An unexpected server error occurred.');
});

const server = app.listen(Number(process.env.PORT || 8080), () => {
  console.log(`AI I.Z API listening on port ${process.env.PORT || 8080}`);
});
async function shutdown() { server.close(); await pool.end(); }
process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);
