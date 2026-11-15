"use strict";

const express = require("express");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const { validateTargetId, normalizeReadOnlySql } = require("./policy");

function bearerToken(header) {
  if (typeof header !== "string") return null;
  const match = /^Bearer\s+([A-Za-z0-9._~-]+)$/i.exec(header);
  return match ? match[1] : null;
}

function createApp({ auth, pools, version = "1.0.0" }) {
  const app = express();
  app.disable("x-powered-by");
  app.use(helmet({ contentSecurityPolicy: false }));
  app.use(express.json({ limit: "64kb", strict: true }));
  app.use(rateLimit({ windowMs: 60_000, limit: 60, standardHeaders: "draft-8", legacyHeaders: false }));

  app.use("/api", async (req, res, next) => {
    try {
      const token = bearerToken(req.headers.authorization);
      if (!token) return res.status(401).json({ error: "unauthorized" });
      req.identity = await auth.verifyIdToken(token, true);
      if (!req.identity || typeof req.identity.uid !== "string" || !req.identity.uid) {
        return res.status(401).json({ error: "unauthorized" });
      }
      return next();
    } catch {
      return res.status(401).json({ error: "unauthorized" });
    }
  });

  app.get("/api/health", async (req, res) => {
    let postgresConnected = false;
    let activeDatabaseName = "";
    const pool = pools.isolated_dev;
    if (pool) {
      try {
        const result = await pool.query("SELECT current_database() AS name");
        activeDatabaseName = String(result.rows[0]?.name || "");
        postgresConnected = true;
      } catch {
        postgresConnected = false;
      }
    }
    res.json({
      status: postgresConnected ? "ok" : "degraded",
      version,
      dotnetAvailable: false,
      flutterAvailable: false,
      postgresConnected,
      activeDatabaseName,
      lastCheckUtc: new Date().toISOString()
    });
  });

  app.get("/api/db/schema", async (req, res) => {
    const pool = pools.isolated_dev;
    if (!pool) return res.status(503).json({ error: "database_not_configured" });
    try {
      const tablesResult = await pool.query(
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name LIMIT 200"
      );
      const tables = [];
      for (const table of tablesResult.rows) {
        const columnsResult = await pool.query(
          "SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_schema = 'public' AND table_name = $1 ORDER BY ordinal_position",
          [table.table_name]
        );
        const primaryResult = await pool.query(
          "SELECT kcu.column_name FROM information_schema.table_constraints tc JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name AND tc.constraint_schema = kcu.constraint_schema WHERE tc.constraint_type = 'PRIMARY KEY' AND tc.table_schema = 'public' AND tc.table_name = $1",
          [table.table_name]
        );
        const primaryKeys = new Set(primaryResult.rows.map(row => row.column_name));
        tables.push({
          schema: "public",
          name: table.table_name,
          rowEstimate: 0,
          columns: columnsResult.rows.map(column => ({
            name: column.column_name,
            dataType: column.data_type,
            isNullable: column.is_nullable === "YES",
            isPrimaryKey: primaryKeys.has(column.column_name)
          })),
          indexes: [],
          relations: [],
          rlsEnabled: false
        });
      }
      const database = await pool.query("SELECT current_database() AS name, current_setting('server_version') AS version");
      res.json({
        databaseName: database.rows[0]?.name || "",
        engineVersion: database.rows[0]?.version || "",
        isIsolatedTarget: true,
        tables
      });
    } catch {
      res.status(503).json({ error: "schema_unavailable" });
    }
  });

  app.post("/api/db/query-readonly", async (req, res) => {
    let target;
    let sql;
    try {
      target = validateTargetId(req.body?.targetDatabase);
      sql = normalizeReadOnlySql(req.body?.sql);
    } catch (error) {
      return res.status(400).json({ error: "invalid_readonly_query", message: error.message });
    }
    const pool = pools[target];
    if (!pool || target !== "isolated_dev") {
      return res.status(404).json({ error: "database_target_not_found" });
    }

    const client = await pool.connect().catch(() => null);
    if (!client) return res.status(503).json({ error: "database_unavailable" });
    try {
      await client.query("BEGIN READ ONLY");
      await client.query("SET LOCAL statement_timeout = '3000ms'");
      await client.query("SET LOCAL lock_timeout = '1000ms'");
      await client.query("SET LOCAL search_path = public");
      const result = await client.query({
        text: `SELECT * FROM (${sql}) AS aiiz_readonly_result LIMIT 201`,
        rowMode: "array"
      });
      await client.query("ROLLBACK");
      const limited = result.rows.length > 200;
      const rows = limited ? result.rows.slice(0, 200) : result.rows;
      return res.json({
        executed: true,
        rowsAffected: rows.length,
        columns: result.fields.map(field => field.name),
        rows: rows.map(row => row.map(value => value == null ? "" : String(value))),
        impactSummary: "Read-only query completed in an isolated transaction.",
        warnings: limited ? ["Results were limited to 200 rows."] : []
      });
    } catch {
      await client.query("ROLLBACK").catch(() => {});
      return res.status(400).json({ error: "readonly_query_failed" });
    } finally {
      client.release();
    }
  });

  app.post("/api/tests/run", (req, res) => {
    res.status(503).json({
      error: "isolated_test_runner_not_configured",
      message: "Remote test execution remains disabled until an isolated worker is configured."
    });
  });

  app.post("/api/db/execute-approved", (req, res) => {
    res.status(503).json({
      error: "approved_execution_not_configured",
      message: "Approved database mutations remain disabled until server-side approval verification and a restricted migration role are configured."
    });
  });

  app.use((error, req, res, next) => {
    if (res.headersSent) return next(error);
    res.status(400).json({ error: "invalid_request" });
  });

  return app;
}

function loadPools(env = process.env) {
  const { Pool } = require("pg");
  const connectionString = env.DATABASE_URL_READONLY;
  const pools = {};
  if (connectionString) {
    pools.isolated_dev = new Pool({
      connectionString,
      max: 5,
      connectionTimeoutMillis: 5000,
      idleTimeoutMillis: 10000,
      statement_timeout: 4000,
      query_timeout: 5000,
      ssl: env.DATABASE_SSL === "disable" ? false : { rejectUnauthorized: true }
    });
  }
  return pools;
}

async function start() {
  const admin = require("firebase-admin");
  if (!admin.apps.length) admin.initializeApp({ projectId: process.env.FIREBASE_PROJECT_ID });
  const pools = loadPools();
  const app = createApp({ auth: admin.auth(), pools });
  const port = Number(process.env.PORT || 8080);
  const host = process.env.HOST || "0.0.0.0";
  const server = app.listen(port, host, () => {
    process.stdout.write(`AI I.Z Backend Runner listening on ${host}:${port}\n`);
  });
  const shutdown = async () => {
    server.close();
    await Promise.all(Object.values(pools).map(pool => pool.end()));
  };
  process.once("SIGTERM", shutdown);
  process.once("SIGINT", shutdown);
}

if (require.main === module) {
  start().catch(() => {
    process.stderr.write("Backend Runner failed to start. Check server configuration.\n");
    process.exitCode = 1;
  });
}

module.exports = { createApp, bearerToken, loadPools };
