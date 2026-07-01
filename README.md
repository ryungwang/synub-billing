# Synub 결제 서비스 PRD

**제품명:** Synub Billing (app.synub.io 내장 결제 모듈)
**작성:** 2026-06-29
**버전:** v0.1 (MVP)
**한 줄 정의:** 신업의 여러 SaaS 제품을 하나의 통합 계정으로 카드 정기결제(구독)하게 하는 중앙 빌링 모듈.

> 📌 이 문서는 **PRD(원안 비전)**이다. **현재 실제 구현 현황**은 [`docs/architecture/IMPLEMENTATION.md`](docs/architecture/IMPLEMENTATION.md), 문서 인덱스는 [`docs/README.md`](docs/README.md) 참조.
> 구현됨: 카탈로그·구독·정기청구·재시도·웹훅 · PortOne(토스) · **SSO(통합계정·리프레시·서명키영속·탈취감지)** · 조직·역할·초대·개인/회사 컨텍스트 · seat 과금·비례정산 · 결제알림 이메일 · 분산락·멱등키 · 관리자 콘솔. **남은 것: 배포.**

---

## 0. 배경 / 결정 사항

신업은 여러 SaaS 제품(문서분석, 스레드 자동생성 등)을 운영하며, 제품은 계속 추가된다. 사용자는 `app.synub.io`에서 제품을 구경·구독하고, 각 제품(`docs.synub.io`, `threads.synub.io`)은 자기 도메인에 독립 배포된다.

결제는 제품마다 따로 구현하지 않고 **중앙 한 곳에서 처리**한다. 사용자는 카드를 한 번 등록하고 여러 구독을 묶어서 관리한다.

**확정된 설계 결정:**

| 항목 | 결정 | 이유 |
|---|---|---|
| 코드 위치 | app.synub.io 내장 모듈 (별도 레포 아님) | 초기엔 단일 앱이 단순·빠름. 트래픽 커지면 분리 |
| 결제 수단 | 카드 정기결제(빌링)만 | 신업 = 월 구독 모델. MVP 최소 범위 |
| 로그인 | 결제는 customer ID만 외부에서 받음 | 통합계정(SSO)은 별도. 느슨한 결합 |
| PG | 포트원 V2 + KG이니시스 (`inicis_v2`) | 가입비 면제(포트원 경유), 국내+해외 카드, 빌링 지원 |
| 제품↔결제 연동 | API 조회 + 웹훅 통보 | 공유 DB 배제(결합도). 결제 서비스가 single source of truth |

---

## 1. 목표 / 비목표

### 1.1 목표 (MVP)
- 사용자가 카드를 등록하고(빌링키 발급) 제품을 월 구독할 수 있다.
- 매달 정해진 날짜에 자동으로 카드 청구가 일어난다.
- 결제 실패 시 재시도하고, 실패가 지속되면 구독을 중단한다.
- 사용자가 마이페이지에서 구독·결제수단·결제내역을 관리(해지/플랜변경)할 수 있다.
- 제품이 "이 유저가 이 제품 구독 중인지"를 조회할 수 있다.
- 제품이 구독 상태 변화(활성/해지/실패)를 웹훅으로 통보받는다.
- 새 제품 추가가 코드 수정 없이 카탈로그 등록만으로 된다.

### 1.2 비목표 (이번 범위 아님)
- 간편결제(카카오페이·네이버페이 등), 해외카드, 가상계좌
- 통합계정(SSO) 자체 구현 — 외부 customer ID를 신뢰하고 받음
- 쿠폰·프로모션·할인코드
- 무료체험(Free trial) 자동 전환 — 2차로 미룸
- 정산·세금계산서 자동화 — synub-office 회계 자동화 단계로
- 다중 통화 / 환율

---

## 2. 핵심 개념 & 용어

| 용어 | 의미 |
|---|---|
| customer | 신업 통합 계정 유저. 결제는 이 ID(외부 발급)만 보관 |
| product | 신업 제품 1개 (문서분석, 스레드…). 카탈로그 항목 |
| plan | 제품의 요금제 (Basic/Pro…). 가격·청구주기·기능제한 |
| billing key | PG가 발급한 카드 빌링키. 자동청구에 사용 |
| subscription | customer가 특정 plan을 구독하는 상태 |
| payment | 1회 청구·결제 기록 |
| 청구주기 | 월간/연간 |

---

## 3. 사용자 시나리오

### 3.1 신규 구독
1. 사용자가 app.synub.io에서 제품·플랜 선택
2. 환불정책·약관 동의 (center.synub.io 문서 링크)
3. 포트원 결제창에서 카드 등록 → 빌링키 발급
4. 즉시 첫 결제 실행 → 구독 활성화
5. 제품에 `subscription.activated` 웹훅 발송 → 제품이 권한 부여

### 3.2 자동 갱신
1. 매일 스케줄러가 "오늘 청구 대상" 구독 조회
2. 빌링키로 자동청구
3. 성공 → next_billing_date +1주기, payment 기록
4. 실패 → 재시도 큐 등록

### 3.3 결제 실패
1. 1차 실패 → 안내(이메일) + N일 후 재시도
2. 재시도 정책(예: 3일 간격 3회) 모두 실패 → 구독 `past_due` → `suspended`
3. 제품에 `subscription.payment_failed` / `subscription.suspended` 웹훅
4. 사용자가 카드 교체 시 즉시 재청구 → 복구

### 3.4 해지
1. 마이페이지에서 해지 → 즉시 해지 or 기간 만료 시 해지(권장: 만료 시)
2. next_billing_date까지는 이용 가능, 이후 비활성
3. 제품에 `subscription.canceled` 웹훅

### 3.5 플랜 변경
1. 업그레이드 → 즉시 적용(차액 정산 or 다음 주기, MVP는 다음 주기 권장)
2. 다운그레이드 → 다음 주기부터 적용

---

## 4. 도메인 모델

### 4.1 엔티티 개요

```
customer (외부 통합계정 ID 보관)
  └─ billing_key (N) ── 카드 빌링키
  └─ subscription (N)
        ├─ plan (참조)
        │     └─ product (참조)
        ├─ billing_key (참조)
        └─ payment (N) ── 청구 이력
```

### 4.2 테이블 스키마 (PostgreSQL, 멀티테넌트 대비 company_id 포함)

```sql
-- 통합계정 유저 (결제 서비스가 보관하는 최소 정보)
CREATE TABLE customer (
    id            BIGSERIAL PRIMARY KEY,
    company_id    BIGINT      NOT NULL,          -- 멀티테넌트 대비
    external_id   VARCHAR(64) NOT NULL,          -- 통합계정(SSO) 유저 ID
    email         VARCHAR(255),                  -- 영수증·안내 발송용
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (company_id, external_id)
);

-- 제품 카탈로그
CREATE TABLE product (
    id            BIGSERIAL PRIMARY KEY,
    company_id    BIGINT      NOT NULL,
    service_code  VARCHAR(50) NOT NULL,          -- 'doc-analysis', 'threads'
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    domain_url    VARCHAR(255),                  -- docs.synub.io
    demo_url      VARCHAR(255),
    webhook_url   VARCHAR(255),                  -- 이 제품으로 상태변화 통보
    status        VARCHAR(20)  NOT NULL DEFAULT 'active', -- active/hidden
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (company_id, service_code)
);

-- 요금제
CREATE TABLE plan (
    id            BIGSERIAL PRIMARY KEY,
    product_id    BIGINT      NOT NULL REFERENCES product(id),
    plan_code     VARCHAR(50) NOT NULL,          -- 'basic', 'pro'
    name          VARCHAR(100) NOT NULL,
    amount        INTEGER     NOT NULL,          -- 원 단위
    currency      VARCHAR(3)  NOT NULL DEFAULT 'KRW',
    billing_cycle VARCHAR(10) NOT NULL,          -- 'monthly' | 'yearly'
    features      JSONB,                         -- 기능 제한 정의
    status        VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (product_id, plan_code)
);

-- 카드 빌링키
CREATE TABLE billing_key (
    id            BIGSERIAL PRIMARY KEY,
    customer_id   BIGINT      NOT NULL REFERENCES customer(id),
    pg_billing_key VARCHAR(255) NOT NULL,        -- 포트원 빌링키
    card_company  VARCHAR(50),                   -- 표시용
    card_last4    VARCHAR(4),                    -- 마스킹 카드번호 끝 4자리
    status        VARCHAR(20) NOT NULL DEFAULT 'active', -- active/deleted
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 구독
CREATE TABLE subscription (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT     NOT NULL REFERENCES customer(id),
    plan_id         BIGINT     NOT NULL REFERENCES plan(id),
    billing_key_id  BIGINT     NOT NULL REFERENCES billing_key(id),
    status          VARCHAR(20) NOT NULL,         -- active/past_due/suspended/canceled
    started_at      TIMESTAMPTZ NOT NULL,
    next_billing_date DATE      NOT NULL,
    canceled_at     TIMESTAMPTZ,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 결제 이력
CREATE TABLE payment (
    id              BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT     NOT NULL REFERENCES subscription(id),
    pg_payment_id   VARCHAR(255),                 -- 포트원 결제 ID
    amount          INTEGER    NOT NULL,
    status          VARCHAR(20) NOT NULL,         -- paid/failed/refunded
    failure_reason  TEXT,
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

> 멀티테넌트: 신업 메모리 원칙대로 company_id를 핵심 테이블에 둠. 단일 법인 운영 중이라 당장은 고정값이지만 구조는 미리 잡아둠.

---

## 5. 상태 머신 (subscription.status)

```
                ┌──────────┐  결제성공   ┌──────────┐
   구독생성 ───▶│ active   │◀───────────│ past_due │
                └────┬─────┘  카드교체후   └────┬─────┘
                     │ 결제실패              재시도 모두 실패
                     ▼                          ▼
                ┌──────────┐              ┌───────────┐
                │ past_due │─────────────▶│ suspended │
                └──────────┘              └───────────┘
   해지요청 ──▶ canceled (기간만료 시 비활성)
```

- **active**: 정상. 제품 이용 가능
- **past_due**: 결제 실패, 재시도 중. 이용 유예(grace) 가능
- **suspended**: 재시도 모두 실패. 이용 차단
- **canceled**: 해지. next_billing_date까지 이용 후 종료

---

## 6. API 설계

### 6.1 내부 API — 포털(app) → 결제

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/billing/keys` | 빌링키 발급 완료 등록(프론트 결제창 후 콜백) |
| DELETE | `/billing/keys/{id}` | 결제수단 삭제 |
| POST | `/subscriptions` | 구독 생성 + 첫 결제 |
| GET | `/subscriptions?customer={id}` | 내 구독 목록 |
| POST | `/subscriptions/{id}/cancel` | 해지 |
| POST | `/subscriptions/{id}/change-plan` | 플랜 변경 |
| GET | `/payments?customer={id}` | 결제 내역 |

### 6.2 제품용 API — 제품 → 결제 (조회)

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/entitlements?customer={id}&service={code}` | 구독 권한 조회 |

응답 예:
```json
{
  "active": true,
  "plan": "pro",
  "expiresAt": "2026-07-29",
  "features": { "maxDocs": 1000, "api": true }
}
```
인증: 제품↔결제 간 내부 서비스 토큰(API Key 또는 mTLS).

### 6.3 웹훅 — 결제 → 제품 (통보)

이벤트:
- `subscription.activated`
- `subscription.canceled`
- `subscription.payment_failed`
- `subscription.suspended`
- `subscription.plan_changed`

각 product.webhook_url로 POST. 서명(HMAC) 포함, 재시도(지수 백오프).

### 6.4 PG 웹훅 — 포트원 → 결제

포트원에서 결제 승인/실패/취소 통보 수신 → payment 기록 갱신 → 필요 시 제품 웹훅 트리거.

---

## 7. 포트원 V2 빌링 연동 흐름

### 7.1 빌링키 발급 (프론트, Next.js)
```
@portone/browser-sdk/v2 의 requestIssueBillingKey()
  → KG이니시스(inicis_v2) 결제창
  → 카드 등록 → billingKey 발급
  → 결제 서비스 POST /billing/keys 로 전달·저장
```

### 7.2 첫 결제 + 구독 생성 (백엔드, Spring Boot)
```
POST /subscriptions
  → billing_key로 즉시 결제 (POST api.portone.io/payments/{id}/billing-key)
  → 성공 시 subscription active, payment 기록
  → product.webhook_url로 subscription.activated 발송
```

### 7.3 월 자동청구 (스케줄러)
```
@Scheduled (매일 1회)
  → next_billing_date <= today AND status=active 조회
  → 각각 빌링키로 청구
  → 성공: next_billing_date 갱신, payment(paid)
  → 실패: status=past_due, 재시도 큐
```

### 7.4 재시도
```
past_due 구독 → 설정된 간격(예: D+1, D+3, D+5)으로 재청구
  → 성공: active 복귀
  → 모두 실패: suspended + 웹훅
```

---

## 8. 화면 (app.synub.io)

1. **제품 목록** — 카탈로그(product) 기반, 플랜·가격 표시
2. **구독 신청** — 플랜 선택 → 약관/환불정책 동의 → 결제창 → 완료
3. **마이페이지 / 구독 관리** — 내 구독 목록, 해지, 플랜 변경
4. **결제수단 관리** — 등록된 카드, 추가/삭제/대표카드
5. **결제 내역** — payment 목록, 영수증

> 약관·환불정책은 center.synub.io의 기존 문서(`/policies/terms`, `/policies/refund`)를 링크로 연결. 중복 작성 안 함.

---

## 9. 비기능 요구사항

- **보안**: 카드 원번호 비보관(빌링키만). PG 표준 준수. 빌링키는 암호화 저장 권장
- **멱등성**: 결제·웹훅 처리에 멱등키. 중복 청구 방지
- **로그/감사**: 모든 결제 시도·상태변화 기록
- **장애 격리**: 제품 웹훅 실패가 결제 자체를 막지 않음(비동기·재시도)
- **타임존**: 청구일 계산은 Asia/Seoul 기준

---

## 10. 단계별 구현 로드맵

| 단계 | 내용 | 산출물 |
|---|---|---|
| M1 | 도메인 모델 + 스키마 + 카탈로그 CRUD | 테이블, 엔티티, product/plan 등록 |
| M2 | 포트원 V2 빌링키 발급 (테스트 모드) | 카드 등록 → billing_key 저장 |
| M3 | 구독 생성 + 첫 결제 + 자동청구 스케줄러 | subscription/payment, @Scheduled |
| M4 | 결제 실패 재시도 + 상태머신 | past_due/suspended 전이 |
| M5 | 제품용 entitlements API + 웹훅 발신 | 제품 연동 인터페이스 |
| M6 | 포털 UI (구독/마이페이지/결제수단/내역) | Next.js 화면 |
| M7 | KG이니시스 실연동 전환 + 심사 | 운영 채널, 실결제 오픈 |

> M7(실연동)은 서비스 URL에 결제창이 붙어야 심사 통과 → 제품 결제 페이지 완성 후 진행.

---

## 11. 열린 질문 (추후 결정)

- 무료체험 도입 여부·방식 (카드 선등록 후 N일 무료 → 자동전환?)
- 플랜 변경 시 일할 정산(proration) 적용 여부 (MVP는 미적용)
- 연간 결제 할인율
- 해외 사용자 → 글로벌 결제(Paddle 등) 별도 트랙 검토 시점
- 통합계정(SSO) 구축 후 customer.external_id 매핑 확정
- 영수증/세금계산서 발행 (synub-office 회계 자동화와 연계)

---

## 부록 A. 기술 스택

- **백엔드**: Java 21, Spring Boot 3, PostgreSQL 16
- **프론트**: Next.js 15 (App Router), Tailwind v4, shadcn/ui
- **결제**: 포트원 V2 SDK (`@portone/browser-sdk/v2`), KG이니시스(inicis_v2)
- **스케줄링**: Spring `@Scheduled` (또는 분산 환경 시 ShedLock)
- **배포**: Docker, 단일 EC2 (현재 인프라 통합 방향)
