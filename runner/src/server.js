import { Pool } from 'pg';
import { createApp } from './app.js';

const required = ['DATABASE_URL', 'AI_IZ_API_BASE_URL'];
for (const name of required) if (!process.env[name]) throw new Error(`Missing required environment variable: ${name}`);

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  max: 5,
  connectionTimeoutMillis: 5000,
  idleTimeoutMillis: 30_000,
  statement_timeout: 5000,
  query_timeout: 7000,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: true } : undefined
});
const app = createApp({
  pool,
  identityApiUrl: process.env.AI_IZ_API_BASE_URL,
  isolatedTarget: process.env.RUNNER_DATABASE_ISOLATED === 'true',
  schemaAccessUserIds: (process.env.RUNNER_SCHEMA_USER_IDS || '').split(',').map(value => value.trim()).filter(Boolean)
});
const port = Number(process.env.PORT || 8090);
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('PORT must be a valid TCP port.');
const server = app.listen(port, () => console.log(`AI I.Z Backend Runner listening on port ${port}`));

let closing = false;
async function shutdown() {
  if (closing) return;
  closing = true;
  server.close(async () => {
    await pool.end();
    process.exit(0);
  });
  setTimeout(() => process.exit(1), 10_000).unref();
}
process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);