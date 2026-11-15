import test from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { readFile } from 'node:fs/promises';
import { Pool } from 'pg';
import { SignJWT } from 'jose';
import { generateRefreshToken, hashRefreshToken } from '../src/security.js';

const databaseUrl = process.env.TEST_DATABASE_URL;
const baseUrl = 'http://127.0.0.1:43871';
const issuer = 'http://127.0.0.1:43871';
const audience = 'ai-iz-integration-tests';
const secretHex = 'a'.repeat(64);
const secret = Buffer.from(secretHex, 'hex');
const enabled = Boolean(databaseUrl);
const backendDir = fileURLToPath(new URL('..', import.meta.url));
let pool;
let server;
let alice;
let bob;
let aliceToken;
let bobToken;
let aliceRefreshToken;
let bobRefreshToken;

async function createAccessToken(userId, sessionId) {
  return new SignJWT({ sid: sessionId, email: userId + '@example.test' })
    .setProtectedHeader({ alg: 'HS256', typ: 'JWT' })
    .setIssuer(issuer).setAudience(audience).setSubject(userId)
    .setIssuedAt().setExpirationTime('5m').sign(secret);
}

async function waitForServer() {
  for (let attempt = 0; attempt < 60; attempt++) {
    try {
      const response = await fetch(baseUrl + '/api/health');
      if (response.ok) return;
    } catch {}
    await new Promise(resolve => setTimeout(resolve, 200));
  }
  throw new Error('AI I.Z test API did not become healthy.');
}

async function api(path, token, method = 'GET', body) {
  return fetch(baseUrl + path, {
    method,
    headers: {
      ...(token ? { authorization: 'Bearer ' + token } : {}),
      ...(body ? { 'content-type': 'application/json' } : {})
    },
    ...(body ? { body: JSON.stringify(body) } : {})
  });
}

test('PostgreSQL API enforces authenticated tenant ownership and record invariants', { skip: !enabled }, async t => {
  await t.before(async () => {
    pool = new Pool({ connectionString: databaseUrl });
    const schema = await readFile(new URL('../db/schema.sql', import.meta.url), 'utf8');
    await pool.query(schema);
    await pool.query('TRUNCATE security_audit_events, runner_approvals, app_sessions, user_records, external_identities, app_users RESTART IDENTITY CASCADE');
    const a = await pool.query(
      "INSERT INTO app_users(email,email_verified,display_name) VALUES('alice@example.test',true,'Alice') RETURNING id"
    );
    const b = await pool.query(
      "INSERT INTO app_users(email,email_verified,display_name) VALUES('bob@example.test',true,'Bob') RETURNING id"
    );
    alice = a.rows[0].id;
    bob = b.rows[0].id;
    const aliceSession = '11111111-1111-4111-8111-111111111111';
    aliceRefreshToken = generateRefreshToken();
    bobRefreshToken = generateRefreshToken();
    const bobSession = '22222222-2222-4222-8222-222222222222';
    await pool.query(
      'INSERT INTO app_sessions(id,user_id,family_id,refresh_token_hash,expires_at) VALUES($1,$2,$3,$4,now()+interval \'1 day\'),($5,$6,$7,$8,now()+interval \'1 day\')',
      [aliceSession, alice, '33333333-3333-4333-8333-333333333333', hashRefreshToken(aliceRefreshToken),
       bobSession, bob, '44444444-4444-4444-8444-444444444444', hashRefreshToken(bobRefreshToken)]
    );
    aliceToken = await createAccessToken(alice, aliceSession);
    bobToken = await createAccessToken(bob, bobSession);
    server = spawn(process.execPath, ['src/server.js'], {
      cwd: backendDir,
      env: {
        ...process.env,
        DATABASE_URL: databaseUrl,
        GOOGLE_OIDC_CLIENT_ID: 'integration-test-client',
        ACCESS_TOKEN_HMAC_SECRET_HEX: secretHex,
        ACCESS_TOKEN_ISSUER: issuer,
        ACCESS_TOKEN_AUDIENCE: audience,
        PORT: '43871',
        NODE_ENV: 'test'
      },
      stdio: 'ignore'
    });
    await waitForServer();
  });

  await t.after(async () => {
    if (server && server.exitCode === null) {
      const exited = new Promise(resolve => server.once('exit', resolve));
      server.kill('SIGTERM');
      await exited;
    }
    await pool?.end();
  });

  await t.test('unauthenticated requests are rejected', async () => {
    const response = await api('/api/data/tasks');
    assert.equal(response.status, 401);
  });

  await t.test('client cannot assign records to another user', async () => {
    const response = await api('/api/data/tasks/task_tenant_test', aliceToken, 'PUT', {
      userId: bob, repo: 'owner/repo', branch: 'main', title: 'Tenant test', prompt: 'Check isolation',
      status: 'queued', currentStepIndex: 0, totalSteps: 1, progressPercent: 0,
      lastCompletedStep: '', lastResultSummary: '', lastError: '', modifiedFiles: [], nextSteps: []
    });
    assert.equal(response.status, 200);
    const result = await response.json();
    assert.equal(result.record.userId, alice);
    assert.equal((await api('/api/data/tasks/task_tenant_test', bobToken)).status, 404);
    const aliceList = await (await api('/api/data/tasks', aliceToken)).json();
    assert.equal(aliceList.records.length, 1);
  });

  await t.test('record schemas reject out-of-range values', async () => {
    const response = await api('/api/data/tasks/task_invalid', aliceToken, 'PUT', {
      repo: 'owner/repo', branch: 'main', title: 'Invalid', prompt: 'Bad progress',
      status: 'queued', currentStepIndex: 0, totalSteps: 1, progressPercent: 101,
      lastCompletedStep: '', lastResultSummary: '', lastError: '', modifiedFiles: [], nextSteps: []
    });
    assert.equal(response.status, 400);
  });

  await t.test('operation logs are append-only, including duplicate concurrent identifiers', async () => {
    const log = { category: 'test', tier: 1, action: 'test', details: '', outcome: 'success', repo: 'owner/repo' };
    assert.equal((await api('/api/data/operation_logs/log_immutable', aliceToken, 'PUT', log)).status, 200);
    assert.equal((await api('/api/data/operation_logs/log_immutable', aliceToken, 'PUT', { ...log, details: 'changed' })).status, 409);
    assert.equal((await api('/api/data/operation_logs/log_immutable', aliceToken, 'DELETE')).status, 403);
  });

  await t.test('approval state transitions cannot skip approval or reopen terminal records', async () => {
    const pending = {
      taskId: 'task_1', tier: 2, actionType: 'git_push', title: 'Push',
      description: 'Approval', payloadPreview: '', targetResource: 'owner/repo',
      status: 'pending', decisionNote: ''
    };
    assert.equal((await api('/api/data/approvals/appr_state', aliceToken, 'PUT', { ...pending, status: 'execution_reported' })).status, 409);
    assert.equal((await api('/api/data/approvals/appr_state', aliceToken, 'PUT', pending)).status, 200);
    assert.equal((await api('/api/data/approvals/appr_state', aliceToken, 'PUT', { ...pending, status: 'execution_reported' })).status, 409);
    assert.equal((await api('/api/data/approvals/appr_state', aliceToken, 'PUT', { ...pending, status: 'approved' })).status, 200);
    assert.equal((await api('/api/data/approvals/appr_state', aliceToken, 'PUT', { ...pending, status: 'execution_reported' })).status, 200);
    assert.equal((await api('/api/data/approvals/appr_state', aliceToken, 'PUT', { ...pending, status: 'approved' })).status, 409);
  });


  await t.test('server approvals bind one exact operation and target and can be consumed only once', async () => {
    const operation = { kind: 'db_read', resource: 'schema_summary', options: { includeIndexes: true } };
    const created = await api('/api/runner/approvals', aliceToken, 'POST', {
      actionType: 'db_sql', targetResource: 'isolated-db:dev-01',
      operation, preview: 'Read-only schema inspection on isolated development database'
    });
    assert.equal(created.status, 201);
    const approval = (await created.json()).approval;
    assert.equal(approval.status, 'pending');
    assert.match(approval.operation_hash, /^[0-9a-f]{64}$/);

    assert.equal((await api(`/api/runner/approvals/${approval.id}/decision`, bobToken, 'POST', { decision: 'approved' })).status, 404);
    const approved = await api(`/api/runner/approvals/${approval.id}/decision`, aliceToken, 'POST', { decision: 'approved', note: 'Reviewed exact target and scope' });
    assert.equal(approved.status, 200);

    const mismatch = await api(`/api/runner/approvals/${approval.id}/consume`, aliceToken, 'POST', {
      actionType: 'db_sql', targetResource: 'isolated-db:production', operationHash: approval.operation_hash
    });
    assert.equal(mismatch.status, 409);
    assert.equal((await mismatch.json()).error.code, 'APPROVAL_BINDING_MISMATCH');

    const consumed = await api(`/api/runner/approvals/${approval.id}/consume`, aliceToken, 'POST', {
      actionType: 'db_sql', targetResource: 'isolated-db:dev-01', operationHash: approval.operation_hash
    });
    assert.equal(consumed.status, 200);
    const consumedBody = (await consumed.json()).approval;
    assert.equal(consumedBody.status, 'consumed');
    assert.deepEqual(consumedBody.operation, operation);
    const listed = await (await api('/api/runner/approvals', aliceToken)).json();
    assert.equal(listed.approvals.length, 1);
    const bobListResponse = await api('/api/runner/approvals', bobToken);
    const bobList = await bobListResponse.json();
    assert.equal(bobList.approvals.length, 0);
    const reported = await api(`/api/runner/approvals/${approval.id}/report`, aliceToken, 'POST', { outcome: 'success', note: 'Git provider returned success' });
    assert.equal(reported.status, 200);
    const reportBody = await reported.json();
    assert.equal(reportBody.approval.status, 'execution_reported');
    assert.equal(reportBody.executionVerified, false);

    const replay = await api(`/api/runner/approvals/${approval.id}/consume`, aliceToken, 'POST', {
      actionType: 'db_sql', targetResource: 'isolated-db:dev-01', operationHash: approval.operation_hash
    });
    assert.equal(replay.status, 409);
  });


  await t.test('Studio projects persist per account and cannot cross tenant boundaries', async () => {
    const project = {
      projectName: 'durable-studio-project',
      environment: 'WEB_REACT_NEXT',
      primaryLanguage: 'TYPESCRIPT',
      description: 'Integration test project',
      recommendedModel: {},
      alternateModels: [],
      files: [
        { path: 'src/index.ts', language: 'TYPESCRIPT', content: 'export const ready = true;', purpose: 'Entry point', isEntrypoint: true },
        { path: 'README.md', language: 'TEXT', content: '# Project', purpose: 'Documentation', isEntrypoint: false }
      ],
      buildInstructions: 'npm run build',
      runInstructions: 'npm run dev',
      testInstructions: 'npm test'
    };
    const saved = await api('/api/data/studio_projects/studio_project_1', aliceToken, 'PUT', project);
    assert.equal(saved.status, 200);
    const loaded = await api('/api/data/studio_projects/studio_project_1', aliceToken);
    assert.equal(loaded.status, 200);
    assert.equal((await loaded.json()).record.files[0].path, 'src/index.ts');
    const listed = await api('/api/data/studio_projects', aliceToken);
    assert.equal((await listed.json()).records.length, 1);
    assert.equal((await api('/api/data/studio_projects/studio_project_1', bobToken)).status, 404);
    assert.equal((await api('/api/data/studio_projects/studio_project_1', bobToken, 'DELETE')).status, 204);
    assert.equal((await api('/api/data/studio_projects/studio_project_1', aliceToken)).status, 200);
    assert.equal((await api('/api/data/studio_projects/studio_project_1', aliceToken, 'DELETE')).status, 204);
    assert.equal((await api('/api/data/studio_projects/studio_project_1', aliceToken)).status, 404);

    const unsafe = await api('/api/data/studio_projects/studio_project_unsafe', aliceToken, 'PUT', {
      ...project,
      files: [{ ...project.files[0], path: '../outside.ts' }]
    });
    assert.equal(unsafe.status, 400);
  });

  await t.test('logout revokes the refresh-token family without relying on an access token', async () => {
    const response = await api('/api/auth/logout', null, 'POST', { refreshToken: aliceRefreshToken });
    assert.equal(response.status, 204);
    const session = await pool.query('SELECT revoked_at FROM app_sessions WHERE user_id=$1', [alice]);
    assert.ok(session.rows[0].revoked_at);
    assert.equal((await api('/api/data/tasks', aliceToken)).status, 401);
    assert.equal((await api('/api/data/tasks', bobToken)).status, 200);
  });
  await t.test('refresh rotates tokens and revokes the family on token reuse', async () => {
    const first = await api('/api/auth/refresh', null, 'POST', { refreshToken: bobRefreshToken });
    assert.equal(first.status, 200);
    const rotated = await first.json();
    assert.ok(rotated.accessToken);
    assert.ok(rotated.refreshToken);
    assert.notEqual(rotated.refreshToken, bobRefreshToken);
    assert.equal((await api('/api/data/tasks', rotated.accessToken)).status, 200);

    const replay = await api('/api/auth/refresh', null, 'POST', { refreshToken: bobRefreshToken });
    assert.equal(replay.status, 401);
    const replayBody = await replay.json();
    assert.equal(replayBody.error.code, 'REFRESH_REUSE_DETECTED');
    assert.equal((await api('/api/data/tasks', rotated.accessToken)).status, 401);

    const revokedReplacement = await api('/api/auth/refresh', null, 'POST', { refreshToken: rotated.refreshToken });
    assert.equal(revokedReplacement.status, 401);
  });

});
