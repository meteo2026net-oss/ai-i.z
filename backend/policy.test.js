"use strict";

const { test } = require("node:test");
const assert = require("node:assert/strict");
const { validateTargetId, normalizeReadOnlySql } = require("./policy");

test("accepts a simple read-only SELECT and removes one trailing semicolon", () => {
  assert.equal(normalizeReadOnlySql(" SELECT id, name FROM public.projects; "), "SELECT id, name FROM public.projects");
});

test("rejects writes, DDL, privileged functions, and multiple statements", () => {
  for (const sql of [
    "INSERT INTO projects VALUES (1)",
    "UPDATE projects SET name = 'x'",
    "DELETE FROM projects",
    "DROP TABLE projects",
    "SELECT * FROM projects; DELETE FROM projects",
    "SELECT pg_sleep(20)",
    "SELECT * FROM projects FOR UPDATE",
    "WITH changed AS (DELETE FROM projects RETURNING *) SELECT * FROM changed",
    "SELECT * FROM projects -- comment"
  ]) assert.throws(() => normalizeReadOnlySql(sql), undefined, sql);
});

test("rejects catalog access and malformed target identifiers", () => {
  assert.throws(() => normalizeReadOnlySql("SELECT * FROM pg_catalog.pg_tables"));
  assert.throws(() => validateTargetId("../production"));
  assert.equal(validateTargetId("isolated_dev"), "isolated_dev");
});
