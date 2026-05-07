-- PostgreSQL 확장 선언
-- gen_random_uuid()는 PostgreSQL 13+에서 내장 제공되지만,
-- 하위 호환성 및 명시적 의존성 선언을 위해 pgcrypto를 먼저 활성화한다.
-- IF NOT EXISTS: 이미 활성화된 환경에서도 안전하게 재실행 가능.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
