-- 빌링 company_id 제거 — "빌링 운영사(신업) 테넌트"용 예약 컬럼이나 단일 운영사라 항상 1로 고정.
-- 고객 조직은 org_code(organization)로 관리하며, office의 company_id(고객 회사 테넌트)와 이름만 같아
-- 혼동을 유발하므로 걷어낸다. 유니크는 운영사 축을 뺀 자연키로 재설정.
ALTER TABLE customer DROP CONSTRAINT customer_company_id_external_id_key;
ALTER TABLE customer ADD CONSTRAINT customer_external_id_key UNIQUE (external_id);
ALTER TABLE customer DROP COLUMN company_id;

ALTER TABLE product DROP CONSTRAINT product_company_id_service_code_key;
ALTER TABLE product ADD CONSTRAINT product_service_code_key UNIQUE (service_code);
ALTER TABLE product DROP COLUMN company_id;

ALTER TABLE organization DROP COLUMN company_id;
