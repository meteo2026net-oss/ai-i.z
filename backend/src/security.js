import { createHash, randomBytes } from 'node:crypto';

export function hashRefreshToken(token) {
  if (typeof token !== 'string' || token.length < 50 || token.length > 256) {
    throw new TypeError('Refresh token must be a high-entropy opaque string.');
  }
  return createHash('sha256').update(token, 'utf8').digest();
}

export function generateRefreshToken() {
  return randomBytes(48).toString('base64url');
}

export function isValidSigningSecretHex(value) {
  return typeof value === 'string' && /^[a-f0-9]{64}$/i.test(value);
}
