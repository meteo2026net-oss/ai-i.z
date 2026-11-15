"use strict";

const BLOCKED_SQL = /\b(?:insert|update|delete|merge|upsert|truncate|alter|create|drop|grant|revoke|comment|copy|call|do|execute|prepare|deallocate|vacuum|analyze|refresh|listen|notify|set|reset|into|for\s+(?:update|share)|pg_sleep|pg_read_file|pg_read_binary_file|pg_write_file|pg_ls_dir|pg_ls_logdir|pg_ls_waldir|pg_cancel_backend|pg_terminate_backend|pg_notify|pg_advisory_lock|pg_advisory_unlock|pg_try_advisory_lock|pg_reload_conf|pg_rotate_logfile|pg_stat_reset|set_config|dblink|lo_import|lo_export|nextval|setval)\b/i;
const TARGET_ID = /^[a-zA-Z][a-zA-Z0-9_-]{0,63}$/;

function validateTargetId(value) {
  if (typeof value !== "string" || !TARGET_ID.test(value)) {
    throw new Error("Invalid database target.");
  }
  return value;
}

function normalizeReadOnlySql(value) {
  if (typeof value !== "string" || value.trim().length === 0 || value.length > 12000) {
    throw new Error("SQL must be a non-empty string no longer than 12000 characters.");
  }

  let sql = value.trim();
  if (sql.endsWith(";")) sql = sql.slice(0, -1).trimEnd();

  if (!sql || sql.includes(";") || /--|\/\*|\*\//.test(sql)) {
    throw new Error("Only one comment-free SQL statement is accepted.");
  }
  if (!/^select\b/i.test(sql) || /\bwith\b/i.test(sql) || BLOCKED_SQL.test(sql)) {
    throw new Error("Only a restricted SELECT statement is allowed.");
  }
  if (/\b(?:information_schema|pg_catalog)\b/i.test(sql)) {
    throw new Error("System catalog access is not available through this endpoint.");
  }
  return sql;
}

module.exports = { validateTargetId, normalizeReadOnlySql };
