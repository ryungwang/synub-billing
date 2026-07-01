-- PG 실연동(빌링키 발급·정기청구)에 필요한 고객 전화번호. 카드 등록 시 수집.
ALTER TABLE customer ADD COLUMN phone VARCHAR(20);
