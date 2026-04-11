-- ============================================================
-- V2__seed_admin_user.sql
-- Creates a default admin user for development.
-- Password: Admin@1234  (BCrypt strength 12)

INSERT INTO users (id, email, password, display_name, role, enabled)
VALUES (
    gen_random_uuid(),
    'admin@survey.local',
    '$2a$12$o2lGLozXQSHBOT9PACz/4.bL5LQD4oAIOOmtqYmfCFXPRHRsG5SAi',
    'System Admin',
    'ADMIN',
    true
)
ON CONFLICT (email) DO NOTHING;

