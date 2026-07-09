-- 플랜 변경 예약. 다운그레이드(및 결제주기 변경)는 다음 결제일에 반영한다.
-- entitlement는 현재 plan_id를 그대로 읽고(이용 중 기간엔 결제한 플랜을 보장),
-- BillingEngine이 갱신 결제 성공 시 pending_plan_id를 plan_id로 스왑하고 비운다.
-- 업그레이드는 즉시 전환(차액 즉시청구)이라 이 컬럼을 쓰지 않는다.
ALTER TABLE subscription ADD COLUMN pending_plan_id BIGINT REFERENCES plan (id);
