export function createSessionVerifier({ identityApiUrl, fetchImpl = fetch, timeoutMs = 5000 }) {
  if (!identityApiUrl) throw new Error('AI_IZ_API_BASE_URL is required.');
  const base = new URL(identityApiUrl);
  if (base.protocol !== 'https:' && !(process.env.NODE_ENV !== 'production' && ['localhost', '127.0.0.1'].includes(base.hostname))) {
    throw new Error('AI_IZ_API_BASE_URL must use HTTPS outside local development.');
  }

  return async function verifySession(req, res, next) {
    const match = /^Bearer ([A-Za-z0-9._~-]{20,8192})$/.exec(req.get('authorization') || '');
    if (!match) return res.status(401).json({ error: { code: 'AUTH_REQUIRED', message: 'A valid AI I.Z access token is required.' } });
    try {
      const response = await fetchImpl(new URL('/api/me', base), {
        method: 'GET',
        headers: { authorization: `Bearer ${match[1]}`, accept: 'application/json' },
        signal: AbortSignal.timeout(timeoutMs)
      });
      if (!response.ok) return res.status(401).json({ error: { code: 'SESSION_REJECTED', message: 'The AI I.Z session is invalid, expired, or revoked.' } });
      const body = await response.json();
      const user = body?.user;
      if (!user || typeof user.id !== 'string' || !user.id) {
        return res.status(401).json({ error: { code: 'SESSION_REJECTED', message: 'The identity service returned an invalid session profile.' } });
      }
      req.aiIzUser = Object.freeze({ id: user.id, email: typeof user.email === 'string' ? user.email : '' });
      next();
    } catch {
      return res.status(503).json({ error: { code: 'IDENTITY_UNAVAILABLE', message: 'AI I.Z identity verification is temporarily unavailable.' } });
    }
  };
}