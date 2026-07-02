-- Synub Works(그룹웨어, service_code=office, product_id=3) 판매 SKU 확정 — 2티어.
--   Basic = 전 직원 코어: 전자결재·문서·인사·협업
--   Pro   = Basic 전체 + 재무/경리: 회계 복식부기 기장 · 부가세/법인세 산출 · 세금계산서 내역
-- 제품명·설명은 어드민 메타(관리자 콘솔)에서 관리하므로 여기서 건드리지 않는다. 플랜(가격·기능)만 확정.
-- 외부 자동연동(홈택스 전자신고/전자세금계산서 발급/뱅킹)은 "준비중"이라 판매 기능 목록에서 제외(수기·산출만 명시).

-- 시드가 명시 id로 삽입했으므로 IDENTITY 시퀀스를 현재 최대값으로 맞춘 뒤 자동 할당 삽입(어느 환경이든 id 충돌 없음).
SELECT setval(pg_get_serial_sequence('plan', 'id'), (SELECT MAX(id) FROM plan));

-- Basic 월/년 추가 (id는 IDENTITY 자동 할당)
INSERT INTO plan (product_id, plan_code, name, tagline, amount, billing_cycle, features, is_highlight, sort_order) VALUES
 (3, 'basic',        'Basic',      '전자결재·문서·인사·협업',  19000,  'monthly',
   '["전자결재(상신·결재선·전결·대결)","문서함(버전·법정보존·권한)","인사·근태·휴가·급여","협업(메신저·게시판·일정·자원예약)"]'::jsonb, false, 1),
 (3, 'basic_yearly', 'Basic 연간',  '2개월 무료 (17% 할인)',     190000, 'yearly',
   '["Basic 전체 기능","연간 일괄 결제"]'::jsonb, false, 2);

-- Pro 문구·기능 갱신(회계·세무 포함 명시). 가격(39,000/390,000)은 유지.
UPDATE plan SET
  tagline = '+ 회계·세무 (재무/경리)',
  features = '["Basic 전체 기능","회계 복식부기 기장·전표·재무제표","부가세·법인세 산출","세금계산서 내역·신고자료·PDF","법인카드·자산·예산·은행대사"]'::jsonb,
  is_highlight = true,
  sort_order = 3
  WHERE product_id = 3 AND plan_code = 'pro';

UPDATE plan SET
  features = '["Pro 전체 기능","연간 일괄 결제","우선 지원"]'::jsonb,
  sort_order = 4
  WHERE product_id = 3 AND plan_code = 'yearly';
