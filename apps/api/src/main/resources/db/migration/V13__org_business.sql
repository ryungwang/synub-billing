-- 조직(회사) 사업자 인증. 마구잡이/도용 등록 방지:
--  (1) 사업자등록번호 형식·체크섬·국세청 상태(실존)  (2) 사업자등록증 서류 제출 + 관리자 심사(소유권).
-- 인증 완료(verified) 전에는 결제·구독 등 민감 작업 차단.
ALTER TABLE organization ADD COLUMN business_no    VARCHAR(20);
ALTER TABLE organization ADD COLUMN business_doc   VARCHAR(255);           -- 사업자등록증 파일 키
ALTER TABLE organization ADD COLUMN verify_status  VARCHAR(20) NOT NULL DEFAULT 'pending';  -- pending|verified|rejected
ALTER TABLE organization ADD COLUMN reject_reason  VARCHAR(255);
ALTER TABLE organization ADD COLUMN verified_at    TIMESTAMPTZ;

-- 같은 사업자번호로 중복 회사 생성 방지(정규화된 숫자 기준).
CREATE UNIQUE INDEX uq_org_business_no ON organization(business_no) WHERE business_no IS NOT NULL;

-- 기존(사업자번호 없이 생성된) 조직은 요건 도입 전 생성분이므로 인증완료로 승격(데모 무결성).
UPDATE organization SET verify_status = 'verified', verified_at = now() WHERE business_no IS NULL;
