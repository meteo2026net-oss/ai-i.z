import test from 'node:test';
import assert from 'node:assert/strict';
import { generateRefreshToken, hashRefreshToken, isValidSigningSecretHex } from '../src/security.js';

test('refresh tokens are high entropy URL-safe opaque values', () => {
  const first = generateRefreshToken();
  const second = generateRefreshToken();
  assert.match(first, /^[A-Za-z0-9_-]{64}$/);
  assert.notEqual(first, second);
});

test('refresh token hashes are deterministic binary SHA-256 digests', () => {
  const token = generateRefreshToken();
  const one = hashRefreshToken(token);
  const two = hashRefreshToken(token);
  assert.equal(one.length, 32);
  assert.deepEqual(one, two);
  assert.notDeepEqual(one, hashRefreshToken(generateRefreshToken()));
});

test('rejects low-entropy or malformed refresh token input', () => {
  assert.throws(() => hashRefreshToken('short'), TypeError);
  assert.throws(() => hashRefreshToken('x'.repeat(300)), TypeError);
  assert.throws(() => hashRefreshToken(null), TypeError);
});

test('accepts only 32-byte hex signing secrets', () => {
  assert.equal(isValidSigningSecretHex('a'.repeat(64)), true);
  assert.equal(isValidSigningSecretHex('A'.repeat(64)), true);
  assert.equal(isValidSigningSecretHex('a'.repeat(63)), false);
  assert.equal(isValidSigningSecretHex('g'.repeat(64)), false);
});
