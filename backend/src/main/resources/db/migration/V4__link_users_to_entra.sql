-- Sign-in moved to Microsoft Entra ID, so local passwords are no longer used.
ALTER TABLE app_users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE app_users ADD COLUMN entra_object_id VARCHAR(64) UNIQUE;
ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_role_check;
ALTER TABLE app_users ADD CONSTRAINT app_users_role_check CHECK (role IN ('AGENT', 'ADJUSTER', 'MANAGER'));
