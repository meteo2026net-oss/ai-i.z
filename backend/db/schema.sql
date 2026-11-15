-- AI I.Z owns identity and session state. No Firebase identifiers or tokens.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS app_users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  display_name TEXT NOT NULL DEFAULT '',
  email TEXT NOT NULL,
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  avatar_url TEXT,
  status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'disabled')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS external_identities (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
  provider TEXT NOT NULL CHECK (provider = 'google'),
  provider_subject TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (provider, provider_subject),
  UNIQUE (user_id, provider)
);

CREATE TABLE IF NOT EXISTS app_sessions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
  family_id UUID NOT NULL,
  refresh_token_hash BYTEA NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  replaced_by UUID REFERENCES app_sessions(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_used_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS app_sessions_user_idx ON app_sessions(user_id);
CREATE INDEX IF NOT EXISTS app_sessions_family_idx ON app_sessions(family_id);

CREATE TABLE IF NOT EXISTS security_audit_events (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
  event_type TEXT NOT NULL,
  session_id UUID,
  details JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS security_audit_events_created_idx ON security_audit_events(created_at DESC);


-- User-owned application records. Ownership is always derived from the
-- authenticated server session; clients never choose user_id.
CREATE TABLE IF NOT EXISTS user_records (
  user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
  collection TEXT NOT NULL CHECK (collection IN ('settings','tasks','approvals','chat_messages','operation_logs')),
  record_id TEXT NOT NULL CHECK (length(record_id) BETWEEN 1 AND 128),
  payload JSONB NOT NULL CHECK (jsonb_typeof(payload) = 'object'),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, collection, record_id)
);
CREATE INDEX IF NOT EXISTS user_records_collection_updated_idx
  ON user_records(user_id, collection, updated_at DESC);


-- Immutable, user-bound approvals for future Runner operations. No operation is executable
-- until a Runner endpoint independently validates and consumes this exact approval.
CREATE TABLE IF NOT EXISTS runner_approvals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
  action_type TEXT NOT NULL CHECK (action_type IN (
    'git_push','create_pr','staging_deploy','service_config',
    'db_sql','db_rls_grants','db_migration','merge_pr','prod_deploy'
  )),
  target_resource TEXT NOT NULL CHECK (length(target_resource) BETWEEN 1 AND 300),
  operation JSONB NOT NULL CHECK (jsonb_typeof(operation) = 'object'),
  operation_hash CHAR(64) NOT NULL CHECK (operation_hash ~ '^[0-9a-f]{64}$'),
  preview TEXT NOT NULL CHECK (length(preview) BETWEEN 1 AND 4000),
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','approved','rejected','consumed','expired')),
  decision_note TEXT NOT NULL DEFAULT '' CHECK (length(decision_note) <= 500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL,
  decided_at TIMESTAMPTZ,
  consumed_at TIMESTAMPTZ,
  CONSTRAINT runner_approvals_expiry_check CHECK (expires_at > created_at)
);
CREATE INDEX IF NOT EXISTS runner_approvals_user_status_idx
  ON runner_approvals(user_id,status,created_at DESC);
