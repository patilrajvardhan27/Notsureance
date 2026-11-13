-- Run once as the database admin. The login inherits report_reader, which can only SELECT the rpt_* views.
-- Replace the password and keep it in Key Vault, not in this file.
CREATE ROLE powerbi_reader LOGIN PASSWORD 'replace-me' IN ROLE report_reader;
