"use strict";

const { test } = require("node:test");
const assert = require("node:assert/strict");
const { createApp, bearerToken } = require("./server");

async function withServer(app, run) {
  const server = app.listen(0, "127.0.0.1");
  await new Promise(resolve => server.once("listening", resolve));
  const address = server.address();
  try {
    await run(`http://127.0.0.1:${address.port}`);
  } finally {
    await new Promise(resolve => server.close(resolve));
  }
}

test("Bearer parser rejects missing and malformed credentials", () => {
  assert.equal(bearerToken(undefined), null);
  assert.equal(bearerToken("Basic abc"), null);
  assert.equal(bearerToken("Bearer abc.def_123-xyz"), "abc.def_123-xyz");
});

test("API rejects unauthenticated and invalid Firebase sessions", async () => {
  const app = createApp({
    auth: { verifyIdToken: async token => {
      if (token !== "valid-token") throw new Error("invalid");
      return { uid: "user-1" };
    } },
    pools: {}
  });
  await withServer(app, async base => {
    const anonymous = await fetch(`${base}/api/health`);
    assert.equal(anonymous.status, 401);
    const invalid = await fetch(`${base}/api/health`, {
      headers: { Authorization: "Bearer invalid-token" }
    });
    assert.equal(invalid.status, 401);
    const valid = await fetch(`${base}/api/health`, {
      headers: { Authorization: "Bearer valid-token" }
    });
    assert.equal(valid.status, 200);
    const payload = await valid.json();
    assert.equal(payload.status, "degraded");
    assert.equal(payload.postgresConnected, false);
  });
});

test("read-only endpoint blocks write SQL before touching PostgreSQL", async () => {
  let connectionAttempts = 0;
  const app = createApp({
    auth: { verifyIdToken: async () => ({ uid: "user-1" }) },
    pools: { isolated_dev: { connect: async () => { connectionAttempts++; throw new Error("should not connect"); } } }
  });
  await withServer(app, async base => {
    const response = await fetch(`${base}/api/db/query-readonly`, {
      method: "POST",
      headers: { Authorization: "Bearer valid-token", "Content-Type": "application/json" },
      body: JSON.stringify({ targetDatabase: "isolated_dev", sql: "DELETE FROM accounts" })
    });
    assert.equal(response.status, 400);
    assert.equal(connectionAttempts, 0);
  });
});

test("remote execution and approved mutations stay disabled until isolated controls exist", async () => {
  const app = createApp({
    auth: { verifyIdToken: async () => ({ uid: "user-1" }) },
    pools: {}
  });
  await withServer(app, async base => {
    const headers = { Authorization: "Bearer valid-token", "Content-Type": "application/json" };
    const tests = await fetch(`${base}/api/tests/run`, { method: "POST", headers, body: "{}" });
    const mutations = await fetch(`${base}/api/db/execute-approved`, { method: "POST", headers, body: "{}" });
    assert.equal(tests.status, 503);
    assert.equal(mutations.status, 503);
  });
});
