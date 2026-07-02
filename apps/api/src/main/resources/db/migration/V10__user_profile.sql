-- 사용자 프로필(통합계정 external_id별) — 마이페이지 프로필 사진.
-- avatar_key: 공개 서빙용 식별자(UUID, /avatars/{avatar_key}). storage_key: 내부 저장키(S3/로컬).
-- 등록된 avatar_key만 공개 서빙 → 임의 스토리지 키 노출 방지.
CREATE TABLE user_profile (
    external_id VARCHAR(64) PRIMARY KEY,
    avatar_key  VARCHAR(64),
    storage_key VARCHAR(255),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_user_profile_avatar_key ON user_profile (avatar_key) WHERE avatar_key IS NOT NULL;
