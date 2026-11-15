import test from 'node:test';
import assert from 'node:assert/strict';
import { createApp } from '../src/app.js';

test('schema inspection is forbidden unless the verified user is allowlisted', async () => {
  const app = createApp({
    pool: { query: async () => { throw new Error('schema query must not run'); } },
    identityApiUrl: 'https://identity.example.test',
    fetchImpl: async () => new Response(JSON.stringify({ user: { id: 'ordinary-user' } }), {
      status: 200, headers: { 'content-type': 'application/json' }
    })
  });
  const server = app.listen(0);
  try {
    const response = await fetch(`http://127.0.0.1:${server.address().port}/api/db/schema`, {
      headers: { authorization: 'Bearer abcdefghijklmnopqrstuvwxyz.1234567890.signature' }
    });
    assert.equal(response.status, 403);
    assert.equal((await response.json()).error.code, 'SCHEMA_ACCESS_FORBIDDEN');
  } finally {
    await new Promise(resolve => server.close(resolve));
  }
});

test('remote execution endpoints fail closed and never invoke database queries', async () => {
  let queryCount = 0;
  const app = createApp({
    pool: { query: async () => { queryCount += 1; throw new Error('must not be called'); } },
    identityApiUrl: 'https://identity.example.test',
    fetchImpl: async () => new Response(JSON.stringify({ user: { id: 'user-1' } }), {
      status: 200, headers: { 'content-type': 'application/json' }
    })
  });
  const server = app.listen(0);
  try {
    const address = server.address();
    const headers = {
      authorization: 'Bearer abcdefghijklmnopqrstuvwxyz.1234567890.signature',
      'content-type': 'application/json'
    };
    for (const path of ['/api/tests/run', '/api/db/query-readonly', '/api/db/execute-approved']) {
      const response = await fetch(`http://127.0.0.1:${address.port}${path}`, {
        method: 'POST', headers, body: JSON.stringify({ sql: 'DROP TABLE public.user_records' })
      });
      assert.equal(response.status, 501);
    }
    assert.equal(queryCount, 0);
  } finally {
    await new Promise(resolve => server.close(resolve));
  }
});