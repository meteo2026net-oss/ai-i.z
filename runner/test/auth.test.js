import test from 'node:test';
import assert from 'node:assert/strict';
import { createSessionVerifier } from '../src/auth.js';

function invoke(verifier, authorization) {
  return new Promise(resolve => {
    const req = { get: name => name.toLowerCase() === 'authorization' ? authorization : undefined };
    const res = {
      statusCode: 200, body: null,
      status(code) { this.statusCode = code; return this; },
      json(body) { this.body = body; resolve({ req, res: this, nextCalled: false }); }
    };
    verifier(req, res, () => resolve({ req, res, nextCalled: true }));
  });
}

test('rejects missing bearer credentials without contacting identity API', async () => {
  let called = false;
  const verify = createSessionVerifier({ identityApiUrl: 'https://identity.example.test', fetchImpl: async () => { called = true; } });
  const result = await invoke(verify, '');
  assert.equal(result.res.statusCode, 401);
  assert.equal(called, false);
});

test('accepts only a profile returned by AI I.Z session introspection', async () => {
  let requested;
  const verify = createSessionVerifier({
    identityApiUrl: 'https://identity.example.test',
    fetchImpl: async (url, options) => {
      requested = { url: String(url), options };
      return new Response(JSON.stringify({ user: { id: 'user-123', email: 'user@example.test' } }), {
        status: 200, headers: { 'content-type': 'application/json' }
      });
    }
  });
  const result = await invoke(verify, 'Bearer abcdefghijklmnopqrstuvwxyz.1234567890.signature');
  assert.equal(result.nextCalled, true);
  assert.equal(result.req.aiIzUser.id, 'user-123');
  assert.equal(requested.url, 'https://identity.example.test/api/me');
  assert.equal(requested.options.headers.authorization, 'Bearer abcdefghijklmnopqrstuvwxyz.1234567890.signature');
});

test('rejects revoked or invalid sessions', async () => {
  const verify = createSessionVerifier({
    identityApiUrl: 'https://identity.example.test',
    fetchImpl: async () => new Response('{}', { status: 401 })
  });
  const result = await invoke(verify, 'Bearer abcdefghijklmnopqrstuvwxyz.1234567890.signature');
  assert.equal(result.res.statusCode, 401);
  assert.equal(result.res.body.error.code, 'SESSION_REJECTED');
});

test('fails closed when the identity API is unavailable', async () => {
  const verify = createSessionVerifier({
    identityApiUrl: 'https://identity.example.test',
    fetchImpl: async () => { throw new Error('network down'); }
  });
  const result = await invoke(verify, 'Bearer abcdefghijklmnopqrstuvwxyz.1234567890.signature');
  assert.equal(result.res.statusCode, 503);
  assert.equal(result.res.body.error.code, 'IDENTITY_UNAVAILABLE');
});