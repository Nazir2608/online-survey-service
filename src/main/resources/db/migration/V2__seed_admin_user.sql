-- ============================================================
-- V2__seed_admin_user.sql
-- Creates a default admin user for development.
-- Password: Admin@1234  (BCrypt strength 12)
--
-- IMPORTANT: Change or remove this in production!
-- Generate a new hash: https://bcrypt-generator.com/
-- ============================================================

INSERT INTO users (id, email, password, display_name, role, enabled)
VALUES (
    gen_random_uuid(),
    'admin@survey.local',
    '$2b$12$BNbLARO.yHSWq0Ff0h6tpeMklD./w10rh8PAtvljLkYu4SPwCHnbC',
    'System Admin',
    'ADMIN',
    true
)
ON CONFLICT (email) DO NOTHING;

-- Password above is: Admin@1234
-- To generate your own BCrypt hash (strength 12):
-- echo "yourpassword" | htpasswd -bnBC 12 "" | tr -d ':\n'