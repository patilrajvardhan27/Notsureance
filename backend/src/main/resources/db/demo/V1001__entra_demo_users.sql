-- Demo users moved to Entra test accounts, so the old password hashes were cleared.
UPDATE app_users SET password_hash = NULL;

-- Entra sign-in names must sit in the tenant's own domain, so the demo emails followed it.
UPDATE app_users SET email = replace(email, '@notsureance.dev', '@${demoEmailDomain}');

INSERT INTO app_users (email, full_name, role)
VALUES ('manager@${demoEmailDomain}', 'Grace Okafor', 'MANAGER')
ON CONFLICT (email) DO NOTHING;
