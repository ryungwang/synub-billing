-- 조직 사업자 구분(법인/개인) + 법인등록번호. 회사 만들기에서 법인이면 법인등록번호 입력.
ALTER TABLE organization ADD COLUMN corp_type VARCHAR(10);   -- 'corp'(법인) | 'individual'(개인사업자)
ALTER TABLE organization ADD COLUMN corp_no    VARCHAR(20);  -- 법인등록번호(13자리) — 법인만, 개인은 null
COMMENT ON COLUMN organization.corp_type IS '사업자 구분: corp(법인) | individual(개인사업자)';
COMMENT ON COLUMN organization.corp_no   IS '법인등록번호(13자리) — 법인만. 개인사업자는 null';
