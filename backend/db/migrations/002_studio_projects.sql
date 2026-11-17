-- Add durable AI Studio projects to the existing user-owned record store.
-- Safe to run repeatedly on databases created from earlier schema versions.
ALTER TABLE user_records DROP CONSTRAINT IF EXISTS user_records_collection_check;
ALTER TABLE user_records
  ADD CONSTRAINT user_records_collection_check
  CHECK (collection IN ('settings','tasks','approvals','chat_messages','operation_logs','studio_projects'));
