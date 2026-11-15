import express from 'express';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import { createSessionVerifier } from './auth.js';

const unsupported = (res, code, message) => res.status(501).json({ error: { code, message } });

export function createApp({ pool, identityApiUrl, fetchImpl = fetch, isolatedTarget = false, schemaAccessUserIds = [] }) {
  if (!pool) throw new Error('A dedicated Runner PostgreSQL pool is required.');
  const app = express();
  app.disable('x-powered-by');
  app.use(helmet());
  app.use(express.json({ limit: '16kb', strict: true }));
  app.use('/api', rateLimit({ windowMs: 60_000, limit: 60, standardHeaders: 'draft-8', legacyHeaders: false }));
  app.use('/api', createSessionVerifier({ identityApiUrl, fetchImpl }));

  app.get('/api/health', async (_req, res) => {
    try {
      const result = await pool.query('SELECT current_database() AS database_name, version() AS engine_version');
      res.json({
        status: 'ok',
        version: '0.1.0',
        dotnetAvailable: false,
        flutterAvailable: false,
        postgresConnected: true,
        activeDatabaseName: result.rows[0].database_name,
        lastCheckUtc: new Date().toISOString()
      });
    } catch {
      res.status(503).json({ error: { code: 'DATABASE_UNAVAILABLE', message: 'The Runner database is unavailable.' } });
    }
  });

  const schemaAdmins = new Set(schemaAccessUserIds.filter(id => typeof id === 'string' && id.trim()).map(id => id.trim()));
  app.get('/api/db/schema', async (req, res) => {
    if (!schemaAdmins.has(req.aiIzUser.id)) return res.status(403).json({ error: { code: 'SCHEMA_ACCESS_FORBIDDEN', message: 'Schema inspection is restricted to configured Runner administrators.' } });
    try {
      const [db, tables, columns, indexes, relations] = await Promise.all([
        pool.query('SELECT current_database() AS database_name, version() AS engine_version'),
        pool.query(`SELECT c.oid, n.nspname AS schema_name, c.relname AS table_name,
                          GREATEST(c.reltuples::bigint, 0) AS row_estimate,
                          c.relrowsecurity AS rls_enabled
                   FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind IN ('r','p') AND n.nspname='public'
                   ORDER BY c.relname LIMIT 200`),
        pool.query(`SELECT n.nspname AS schema_name, c.relname AS table_name, a.attname AS column_name,
                          format_type(a.atttypid,a.atttypmod) AS data_type,
                          NOT a.attnotnull AS is_nullable,
                          EXISTS (SELECT 1 FROM pg_index i WHERE i.indrelid=c.oid AND i.indisprimary AND a.attnum=ANY(i.indkey)) AS is_primary_key
                   FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   JOIN pg_attribute a ON a.attrelid=c.oid
                   WHERE c.relkind IN ('r','p') AND n.nspname='public' AND a.attnum>0 AND NOT a.attisdropped
                   ORDER BY c.relname,a.attnum`),
        pool.query(`SELECT schemaname AS schema_name, tablename AS table_name, indexname
                   FROM pg_indexes WHERE schemaname='public' ORDER BY tablename,indexname`),
        pool.query(`SELECT ns.nspname AS schema_name, src.relname AS table_name, con.conname AS constraint_name,
                          sa.attname AS source_column, dst.relname AS target_table, da.attname AS target_column
                   FROM pg_constraint con
                   JOIN pg_class src ON src.oid=con.conrelid
                   JOIN pg_namespace ns ON ns.oid=src.relnamespace
                   JOIN pg_class dst ON dst.oid=con.confrelid
                   JOIN LATERAL unnest(con.conkey) WITH ORDINALITY sk(attnum,ord) ON true
                   JOIN LATERAL unnest(con.confkey) WITH ORDINALITY dk(attnum,ord) ON dk.ord=sk.ord
                   JOIN pg_attribute sa ON sa.attrelid=src.oid AND sa.attnum=sk.attnum
                   JOIN pg_attribute da ON da.attrelid=dst.oid AND da.attnum=dk.attnum
                   WHERE con.contype='f' AND ns.nspname='public'
                   ORDER BY src.relname,con.conname`)
      ]);
      const tableModels = tables.rows.map(table => ({
        schema: table.schema_name,
        name: table.table_name,
        rowEstimate: Number(table.row_estimate),
        columns: columns.rows.filter(c => c.table_name === table.table_name).map(c => ({
          name: c.column_name, dataType: c.data_type, isNullable: c.is_nullable, isPrimaryKey: c.is_primary_key
        })),
        indexes: indexes.rows.filter(i => i.table_name === table.table_name).map(i => i.indexname),
        relations: relations.rows.filter(r => r.table_name === table.table_name).map(r => ({
          constraintName: r.constraint_name, sourceColumn: r.source_column,
          targetTable: r.target_table, targetColumn: r.target_column
        })),
        rlsEnabled: table.rls_enabled
      }));
      res.json({
        databaseName: db.rows[0].database_name,
        engineVersion: db.rows[0].engine_version,
        isIsolatedTarget: isolatedTarget,
        tables: tableModels
      });
    } catch {
      res.status(503).json({ error: { code: 'SCHEMA_UNAVAILABLE', message: 'Database schema inspection is temporarily unavailable.' } });
    }
  });

  app.post('/api/tests/run', (_req, res) =>
    unsupported(res, 'TEST_EXECUTION_DISABLED', 'Remote test execution is disabled until an allowlisted isolated worker is deployed.'));

  app.post('/api/db/query-readonly', (_req, res) =>
    unsupported(res, 'SQL_QUERY_DISABLED', 'SQL execution is disabled until a parser-backed read-only policy and dedicated database role are configured.'));

  app.post('/api/db/execute-approved', (_req, res) =>
    unsupported(res, 'APPROVED_EXECUTION_DISABLED', 'Approved execution is disabled until server-side immutable approvals, target binding, and audited execution are deployed.'));

  app.use((_req, res) => res.status(404).json({ error: { code: 'NOT_FOUND', message: 'The requested Runner route does not exist.' } }));
  return app;
}