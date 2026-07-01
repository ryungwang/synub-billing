-- 국세청 진위확인·대표자 본인인증용. 대표자명·개업일자 + 본인인증 결과.
ALTER TABLE organization ADD COLUMN rep_name    VARCHAR(50);
ALTER TABLE organization ADD COLUMN open_date   VARCHAR(8);     -- 개업일자 YYYYMMDD
ALTER TABLE organization ADD COLUMN rep_verified BOOLEAN NOT NULL DEFAULT false;  -- 대표자 본인인증 완료
