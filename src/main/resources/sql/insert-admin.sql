-- ADMIN 계정 초기 데이터 삽입
-- 비밀번호: admin1234!  (BCryptPasswordEncoder, strength=10)
-- 실행 전 중복 여부 확인 후 삽입

INSERT INTO users (username, nickname, password, email, provider, provider_id, role, deleted, deleted_at, created_at, updated_at)
SELECT '관리자', 'admin', '$2a$10$paoeTS1D0Bwo/.BnhCqIFOF0SRjExk1WwtOZo1Q6a23xUDFA2Ij6a', 'admin@eqh.com', 'LOCAL', NULL, 'ADMIN', false, NULL, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@eqh.com'
);
