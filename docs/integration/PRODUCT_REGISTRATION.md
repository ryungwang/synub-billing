# 제품(서비스) 등록 가이드 — synub-billing

> 이 문서는 **다른 에이전트/개발자가 이 레포를 수정해 새 SaaS 제품(서비스)을 빌링 카탈로그에 등록**할 때
> 반드시 지켜야 할 규칙을 기술한다. 여기 적힌 경로·컨벤션을 벗어나지 말 것.

---

## 0. 한눈에: 무엇을 건드려야 하나

제품 하나를 등록하려면 **최소 1곳(필수) + 최대 3곳**을 수정한다.

| # | 위치 | 필수? | 무엇을 |
|---|------|-------|--------|
| 1 | `apps/api/src/main/resources/db/migration/V{n}__*.sql` | **필수** | `product` + `plan` INSERT (신규 Flyway 마이그레이션) |
| 2 | `apps/web/components/product-icon.tsx` | 권장 | 제품 아이콘 매핑 (없으면 기본 아이콘으로 폴백) |
| 3 | `apps/api/.../service/DtoMapper.java` `USAGE` 맵 | 선택 | 대시보드 사용량 스탠드인 (없으면 usage=null) |

> ❗ **플랜·가격(돈)은 마이그레이션 전용이다.** `CatalogController`는 읽기 전용(`GET /products`).
> **제품 "메타"(이름·설명·분류·domain/demo/onboarding/webhook URL·정렬·노출·조직전용)는 관리자 콘솔에서 편집·신규등록 가능**
> (`/admin/products` GET·POST·PUT, admin 전용). 단 **`service_code`는 생성 시에만 설정(이후 불변)**, **플랜·금액은 절대 관리자 API로 만들지 말 것** — 결제 무결성 때문에 아래 마이그레이션으로만.
> 즉 **새 제품의 플랜/가격은 여전히 이 문서(마이그레이션)** 로, 그 제품의 표시 메타는 관리자 콘솔로.

---

## 1. 등록 = Flyway 마이그레이션 (필수)

### 1.1 규칙 (반드시 준수)

- 파일 위치: `apps/api/src/main/resources/db/migration/`
- 파일명: `V{다음번호}__{설명}.sql` (예: `V5__product_ooffice.sql`) — 언더스코어 **2개**.
- **현재 최신 = `V4`. 다음 신규 번호는 `V5`.** (`ls`로 항상 최신 번호 재확인 후 +1)
- **기존 V1~V4 파일은 절대 수정·삭제 금지.** Flyway는 적용된 마이그레이션을 불변으로 취급 → 내용이 바뀌면 체크섬 불일치로 부팅 실패. 변경은 **항상 새 버전 파일 추가**로만.
- 스키마: Flyway `default-schema = billing`. SQL 안에서 스키마 접두사 없이 테이블명만 쓰면 `billing` 스키마에 들어간다.
- 적용 시점: 백엔드 부팅 시 자동 마이그레이션. 파일 추가 후 **백엔드 재기동**하면 반영된다.

### 1.2 `product` 테이블 컬럼 계약

```
product(
  id            BIGINT IDENTITY PK          -- INSERT에서 생략 권장(자동생성). 명시하면 §1.4 setval 필요
  service_code  VARCHAR(50) NOT NULL         -- 안정적 고유 식별자. service_code UNIQUE (단독)
  name          VARCHAR(100) NOT NULL        -- 화면 표시 한글명 (프론트 아이콘 매핑 키! §2)
  category      VARCHAR(50)                  -- 분류 태그 (예: 생산성/마케팅/재무/CS)
  description   TEXT                         -- 카드에 노출되는 한 줄 설명
  domain_url    VARCHAR(255)                 -- 제품 서비스 URL (예: https://office.synub.io)
  demo_url      VARCHAR(255)                 -- 없으면 null
  webhook_url   VARCHAR(255)                 -- 구독 상태 웹훅 수신 URL. 컨벤션: {domain}/webhooks/billing
  status        VARCHAR(20) NOT NULL 'active'-- 'active' | (숨김은 'inactive' 등, 하드삭제 금지 §5)
  sort_order    INTEGER NOT NULL 0           -- 카탈로그 정렬(오름차순)
  org_only      BOOLEAN NOT NULL false       -- true면 회사(조직) 컨텍스트에서만 구독 가능(예: 그룹웨어)
  onboarding_url VARCHAR(255)                -- 초기설정 온보딩 페이지 URL. 있으면 구독 후 '설정하러 가기' 핸드오프(§7.5) 활성화
)
```

> ⚠️ **`company_id` 컬럼은 없다(V4에서 제거됨).** 빌링은 운영사(신업) 전용 단일 서비스라 테넌트 컬럼을 두지 않는다.
> 고객 회사 격리는 `organization`/`org_code`로 한다(§7, `IDENTITY_AND_ORG_BILLING.md`). 신규 INSERT에 `company_id`를 넣지 말 것.

- **`service_code`**: 소문자-케밥(`doc-analysis`, `office`, `threads`). 한 번 정하면 바꾸지 말 것 —
  `DtoMapper.USAGE` 키, 웹훅 페이로드, 제품측 entitlements 조회가 이 값에 묶인다. **`service_code` 자체가 UNIQUE**(중복 금지).
- **`webhook_url`**: 제품이 구독 상태변화(activated/canceled/payment_failed/suspended/plan_changed)를
  받을 엔드포인트. 없으면 웹훅이 발송돼도 받을 곳이 없다. 컨벤션 `https://{sub}.synub.io/webhooks/billing`.
- **`org_only`**: 그룹웨어처럼 회사만 쓰는 제품은 `true`. 개인 컨텍스트에서 구독 버튼이 막힌다.
- **`onboarding_url`**: 셋업이 필요한 제품(그룹웨어 등)의 초기설정 페이지. 채워두면 결제 완료 후
  서명 핸드오프 링크로 이 URL에 진입시킬 수 있다(§7.5). 즉시이용형 제품은 비워둔다(null).

### 1.3 `plan` 테이블 컬럼 계약

```
plan(
  id            BIGINT IDENTITY PK
  product_id    BIGINT NOT NULL FK→product(id)   -- 어떤 제품의 요금제인지
  plan_code     VARCHAR(50) NOT NULL             -- (product_id, plan_code) UNIQUE. 예: basic/pro/enterprise/yearly
  name          VARCHAR(100) NOT NULL            -- 표시명 (예: Basic, Pro, Pro 연간)
  tagline       VARCHAR(255)                     -- 짧은 부제 (예: "성장하는 팀에 최적")
  amount        INTEGER NOT NULL                 -- 금액. 원(KRW) 정수. 소수·콤마 금지. 19000 = 19,000원
  currency      VARCHAR(3) NOT NULL 'KRW'        -- 기본 KRW
  billing_cycle VARCHAR(10) NOT NULL             -- 'monthly' | 'yearly' (문자열 그대로)
  features      JSONB                            -- 문자열 배열. '["...","..."]'::jsonb 형태로 캐스팅
  is_highlight  BOOLEAN NOT NULL false           -- 카탈로그에서 "추천/강조" 표시할 플랜 1개
  status        VARCHAR(20) NOT NULL 'active'
  sort_order    INTEGER NOT NULL 0               -- 플랜 정렬(오름차순)
)
```

- **`amount`는 원 단위 정수.** `29000` = 29,000원. 부가세 별도/포함 여부는 정책에 맞춰 값 자체에 반영.
- **`features`**: 반드시 유효한 JSON 배열 + `::jsonb` 캐스팅. 예: `'["무제한 분석","API 액세스"]'::jsonb`.
- 제품당 플랜은 1개 이상. 강조 플랜(`is_highlight=true`)은 제품당 1개 권장.

### 1.4 복붙 템플릿 (권장: id 자동생성 방식)

`id`를 명시하지 않고 IDENTITY에 맡기면 시퀀스 충돌 걱정이 없다.
`plan.product_id`는 방금 넣은 제품을 `service_code`로 되찾아 참조한다.

```sql
-- V5__product_ooffice.sql
-- OOffice(그룹웨어) 제품 + 요금제 등록. 그룹웨어는 org_only=true + onboarding_url 지정.

INSERT INTO product (service_code, name, category, description, domain_url, webhook_url, status, sort_order, org_only, onboarding_url)
VALUES ('office', 'OOffice', '그룹웨어',
        '문서·회계·전자결재·인사를 한 곳에서. 팀 협업을 위한 올인원 그룹웨어.',
        'https://office.synub.io', 'https://office.synub.io/webhooks/billing', 'active', 10,
        true, 'https://office.synub.io/onboarding');

INSERT INTO plan (product_id, plan_code, name, tagline, amount, billing_cycle, features, is_highlight, sort_order)
VALUES
 ((SELECT id FROM product WHERE service_code='office'),
  'basic', 'Basic', '소규모 팀', 9900, 'monthly',
  '["문서 관리","전자결재","기본 인사관리"]'::jsonb, false, 1),
 ((SELECT id FROM product WHERE service_code='office'),
  'pro', 'Pro', '성장하는 팀', 19900, 'monthly',
  '["Basic 전체","회계·전표","권한 관리","우선 지원"]'::jsonb, true, 2);
```

> **id를 명시적으로 지정해야 한다면**(기존 V2 시드처럼), INSERT 뒤에 아래를 반드시 붙여
> IDENTITY 시퀀스를 최댓값 뒤로 밀어 이후 자동생성 INSERT와 충돌하지 않게 한다:
> ```sql
> SELECT setval(pg_get_serial_sequence('product','id'), (SELECT max(id) FROM product));
> SELECT setval(pg_get_serial_sequence('plan','id'),    (SELECT max(id) FROM plan));
> ```

---

## 2. 프론트 아이콘 매핑 (권장)

`apps/web/components/product-icon.tsx`의 `PRODUCT_ICON` 맵은 **제품의 `name`(한글 표시명)을 키**로 쓴다.
(서비스코드가 아니라 이름이다 — 주의.) 매핑이 없으면 `FileText`로 폴백된다.

```ts
export const PRODUCT_ICON: Record<string, LucideIcon> = {
  "문서분석 AI": FileText,
  "스레드 자동생성": Hash,
  "OOffice": Building2,   // ← 신규 제품 name과 정확히 일치시켜 추가. 아이콘은 lucide-react에서 import
};
```

- **디자인 시스템 규칙(엄수):** 모든 제품 아이콘은 `bg-primary-subtle`(연한 파랑) + `text-primary`(파랑) 통일.
  아이콘의 **모양**으로만 제품을 구분한다. 임의 색/그라데이션/브랜드 3색을 아이콘에 넣지 말 것.
  (브랜드 3색 큐브는 **로고에만** 쓴다. 상세 규칙은 프로젝트 메모리 `ui-must-be-premium` 참조.)
- 아이콘은 `lucide-react`에서 `import` 후 맵과 상단 import 둘 다에 추가.

---

## 3. 대시보드 사용량 스탠드인 (선택)

`apps/api/.../service/DtoMapper.java`의 `USAGE` 맵은 **`service_code`를 키**로 하는 임시 사용량 데이터다.
없으면 해당 제품의 대시보드 usage가 `null`로 나온다(치명적 아님).

```java
private static final Map<String, UsageDto> USAGE = Map.of(
    "doc-analysis", new UsageDto("문서 분석", "건", 642, 2000),
    "office",       new UsageDto("전표 처리", "건", 1820, 5000)  // ← 신규 service_code 추가
);
```

- **주의:** 이건 데모용 스탠드인이다. 실제로는 각 제품의 usage 보고 API/웹훅으로 채워질 값이며 빌링 도메인 밖이다.
  진짜 사용량 연동 전까지의 표시용이므로, 없어도 등록 자체는 성립한다.
- `Map.of`는 최대 10쌍까지만 지원한다. 항목이 많아지면 `Map.ofEntries(...)`로 바꿀 것.

---

## 4. 테넌트 모델 (company_id 없음)

- 빌링은 **운영사(신업) 전용 단일 서비스**다. 과거 존재하던 `company_id`(항상 1인 운영사 테넌트) 컬럼은
  **혼동을 유발해 V4에서 완전히 제거**했다(`tenant/CurrentTenant.java`도 삭제). 신규 제품·플랜 INSERT에 `company_id`를 넣지 말 것.
- **제품 유일성 = `service_code` 단독 UNIQUE.** 같은 `service_code` 중복 금지.
- **고객 회사 격리는 `organization`/`org_code`로 한다** — 빌링의 company_id가 아니다. 조직·역할·구독 소유는
  `IDENTITY_AND_ORG_BILLING.md` 참조. (제품 서비스가 자기 테넌트를 가르는 키도 `org_code`이지 company_id가 아니다.)

---

## 5. 하지 말 것 / 주의사항

- ❌ 기존 `V1`~`V4` 마이그레이션 수정·삭제 (체크섬 불일치 → 부팅 실패). 항상 새 `V{n}` 추가.
- ❌ 제품/플랜 **하드 삭제**. 이미 구독(`subscription.plan_id`)이 참조 중이면 FK 위반이 나거나 결제 이력이 깨진다.
  판매 중단은 `status`를 `'active'`가 아닌 값으로 바꾸고 카탈로그 조회에서 거르는 방식(소프트) 권장.
- ❌ `features`에 잘못된 JSON(따옴표 누락, 트레일링 콤마) → `::jsonb` 캐스팅 실패로 마이그레이션 오류.
- ❌ `amount`에 콤마·소수점·문자.
- ❌ 관리자 API로 **플랜·금액 생성/수정**(결제 무결성 — 마이그레이션 전용). 관리자 콘솔은 **제품 메타만** 다룬다(`AdminController`/`AdminService.createProduct·updateProduct`, `web/components/admin-products.tsx`).
- ❌ `service_code` 사후 변경(연동 키 불변 — 관리자 수정 시에도 무시됨).
- ⚠️ 현재 시드(`V2`)의 제품 4종(문서분석 AI/스레드 자동생성/회계 자동화/고객지원 데스크)은 **데모 데이터**다.
  실서비스 등록 시, 데모를 남길지/치울지 결정할 것. 치운다면 하드삭제 대신 새 마이그레이션에서
  `status`를 내리거나(권장) 데모 제거 마이그레이션을 별도로 작성.

---

## 6. 등록 후 검증 (필수 절차)

1. DB 컨테이너 확인: `docker start synub-billing-db` (없으면 최초 생성 — README/`project-state` 메모리 참고).
2. 백엔드 재기동 → Flyway가 새 마이그레이션 자동 적용:
   ```bash
   JAVA_HOME=/Users/haru/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
     ./gradlew :apps:api:bootRun        # 로컬 포트 8088 (Mock 모드가 기본)
   ```
   부팅 로그에 `Migrating schema "billing" to version "6 - ..."`가 보이면 성공.
3. 카탈로그 조회로 확인:
   ```bash
   curl -s http://localhost:8088/products | jq '.[] | {serviceCode, name, plans: [.plans[].name]}'
   ```
   새 제품과 플랜이 나오면 등록 완료.
4. 프론트 확인: `PORT=3100 pnpm --filter web dev` → `http://localhost:3100/products`에서
   제품 카드·아이콘·요금제가 의도대로 렌더되는지 육안 확인.

> 마이그레이션이 오류로 실패하면 Flyway가 해당 버전을 `failed`로 기록할 수 있다.
> 로컬에서는 DB를 드롭/재생성해 깨끗이 다시 적용하는 편이 빠르다(운영 DB에는 하지 말 것).

---

## 7. 빌링 ↔ 서비스 연계 (등록한 제품이 구현할 것)

카탈로그에 제품을 등록하는 것으로 "구독 판매"는 되지만, **구독 상태를 실제 권한으로 반영**하려면
제품 서비스 쪽에서 아래 연계를 구현해야 한다. 결합도를 낮추려고 **공유 DB를 쓰지 않고**,
빌링이 single source of truth가 되어 **API(pull) + 웹훅(push)** 두 경로로만 연동한다.

```
  [사용자]  app.synub.io(빌링)에서 카드등록·구독 ──▶ 빌링 DB (구독의 진실)
                                                     │
   ┌── ① pull: 제품이 실시간 권한 조회 ◀─────────────┤
   │   GET /api/entitlements                          │
   ▼                                                  ▼
 [제품 서비스]  office.synub.io 등     ◀── ② push: 상태변화 웹훅 통보 ──
   (자기 도메인에 독립 배포)              POST {product.webhook_url}
```

- **`service_code`가 두 경로를 잇는 키다.** 제품은 자신의 `service_code`(카탈로그 등록값, §1.2)로
  조회하고, 웹훅 페이로드에도 `serviceCode`가 담겨 온다.
- **사용자 식별 = `customerExternalId`.** 통합계정(SSO) 유저 ID(토큰 `sub`). 제품과 빌링이 같은 유저를 이 값으로 맞춘다.
- **조직 식별 = `orgCode`.** 회사 소유 구독은 `orgCode`(예: `SH-ABCDEFGHIJ`)로 그룹핑한다(entitlements·웹훅에 포함).

### 🔐 신원·계정 규약 (제품이 반드시 지킬 것)

로그인은 **SSO(통합계정) 중앙집중**이다. 제품은 **자체 로그인/비밀번호를 만들지 않는다.**

- ❌ 제품 DB에 **login id / password 저장 금지.** 크레덴셜은 오직 `synub_sso.account`에만 있다.
- ✅ 제품은 SSO가 발급한 **JWT 토큰만 검증**해 로그인 확인(공유 DB 없음).
- ✅ 제품의 user/member 테이블은 **크레덴셜 없이** 두고, `external_id`(SSO sub)로 연결한 **로컬 프로필**만 보관:

  ```sql
  -- 예: office.member (그룹웨어)
  external_id   -- SSO 계정 sub(로그인 주체). 비밀번호 없음
  org_code      -- 소속 조직 테넌트(빌링에서 받은 SH-XXXXXXXXXX)
  role          -- 제품 내 역할(대표/관리자/팀원 등) ← 제품 고유
  department, display_name(표시 캐시), status
  UNIQUE(org_code, external_id)
  ```
- **직원 추가 흐름**: 제품에서 직원 추가 → (SSO 계정 프로비저닝 API로) SSO에 계정 생성 → 제품 member 레코드 생성(external_id + org_code + 제품 role) → 직원은 SSO로 로그인.
- 표시용 이메일·이름은 캐시할 수 있으나 **원천은 SSO**다.

#### SSO 계정 프로비저닝 API (직원 추가)

제품이 직원 계정을 SSO에 만든다. **제품은 비밀번호를 만들거나 저장하지 않는다.**

```
POST {SSO}/provision/accounts
Header: X-Service-Key: {서비스 키(SSO_PROVISION_KEY)}
{ "email": "emp@corp.io", "name": "홍길동" }

→ 201 { "externalId":"usr_...", "email":"emp@corp.io", "name":"홍길동",
        "tempPassword":"Ab12Cd34Ef56", "created":true }
```
- `created:true` — 신규 생성. `tempPassword`를 직원에게 전달(직원은 이걸로 로그인).
- `created:false` — 이미 SSO 계정이 있는 사람(예: 이미 synub 이용자) → 기존 계정 연결, `tempPassword:null`.
- 멱등: 같은 이메일 재요청은 항상 기존 계정 반환. 서비스 키 없으면 403.
- 제품은 반환된 `externalId`로 자사 member 생성(비번 저장 금지).

---

### ① Entitlements 조회 API — 제품이 빌링에게 물어봄 (pull)

제품이 "이 유저가 지금 이 서비스를 구독 중인가?"를 **실시간으로** 확인하는 용도.
로그인/기능 접근 시점에 호출해 게이팅한다.

```
GET /api/entitlements?customer={customerExternalId}&service={serviceCode}
Header: X-Service-Key: {서비스 키}     ← 필수(서버-투-서버 인증)
```

> 🔐 **서비스 키 필수.** 이 API는 사용자 토큰이 아닌 **서비스 키**로 인증한다(임의 고객 정보 조회 방지).
> 헤더 `X-Service-Key`에 빌링이 발급한 키(운영 env `SERVICE_API_KEY`, 로컬 기본 `local-service-key`)를 담아
> **서버에서만** 호출할 것(브라우저 노출 금지). 키 없거나 틀리면 `403`.

| 파라미터 | 필수 | 의미 |
|---------|------|------|
| `service` | **필수** | 조회할 제품의 `service_code` (예: `office`) |
| `customer` | 선택 | 통합계정 유저 ID. 생략 시 데모 계정(`demo-user`)으로 조회 |

응답 (`EntitlementDto`):
```jsonc
{
  "active":   true,                       // 지금 이용 권한이 있나 (구독 status가 'active'일 때만 true)
  "plan":     "pro",                      // 현재 플랜 코드 (없으면 null)
  "expiresAt":"2026-07-12",               // 다음 청구일(=현재 이용기간 만료 경계, 없으면 null)
  "features": ["무제한 문서 분석","API 액세스"], // 플랜 features (권한 세분화에 활용)
  "orgCode":  "SH-ABCDEFGHIJ"             // 조직 소유 구독이면 조직코드(개인 구독이면 null) — 이 값으로 테넌트 그룹핑
}
```

동작 규칙(중요):
- 구독이 **`active` 또는 `past_due`**면 해당 구독을 찾아 반환한다. 단 **`active` 필드가 `true`가 되는 건 status가
  `active`일 때뿐**이다. → `past_due`(결제 실패 유예 중)면 구독은 잡히지만 `active:false`로 온다.
  제품은 이때 "유예 배너 노출 + 기능은 잠깐 유지" 같은 정책을 스스로 정할 수 있다.
- 구독이 없거나 `suspended`/`canceled`면 `{active:false, plan:null, expiresAt:null, features:[]}`.
- **판단 기준은 항상 이 API(빌링)다.** 제품이 구독 상태를 자체 저장하더라도, 최종 권한은 빌링 조회로 확정할 것.

#### 사용량 보고 (선택) — 제품이 빌링에 사용량 push

대시보드/구독 화면의 "이번 달 사용량"을 실데이터로 채우려면 제품이 주기적으로 보고한다.
(보고 안 하면 데모 스탠드인 값이 표시된다.)

```
POST /api/usage
Header: X-Service-Key: {서비스 키}
{ "customer":"{externalId}", "service":"{serviceCode}",
  "label":"문서 분석", "unit":"건", "used":1777, "limit":3000 }
```
`(customer, service)` 기준 upsert(최신값 유지). 서비스 키 필수(무인증 403).

---

### ② 웹훅 통보 — 빌링이 제품에게 알려줌 (push)

구독 상태가 바뀌는 **순간** 빌링이 제품의 `product.webhook_url`(§1.2에서 등록한 값)로 POST한다.
제품은 이 웹훅을 받아 권한을 즉시 부여/회수한다. (매번 폴링할 필요 없음.)

발송되는 이벤트:

| `event` | 발생 시점 | 제품이 할 일(권장) |
|---------|-----------|--------------------|
| `subscription.activated` | 구독 시작·결제 성공 | 권한 부여(온보딩) |
| `subscription.plan_changed` | 플랜 변경 | 권한/한도 재설정 |
| `subscription.payment_failed` | 정기결제 실패 | 유예 표시(권한은 정책대로 유지) |
| `subscription.suspended` | 재시도 소진 후 정지 | 권한 회수 |
| `subscription.canceled` | 해지 | 이용기간 종료 시 권한 회수 |

요청 형식:
```
POST {webhook_url}
Content-Type: application/json
X-Synub-Event: subscription.activated
X-Synub-Signature: sha256={payload를 HMAC-SHA256으로 서명한 hex}
```
```jsonc
{
  "event": "subscription.activated",
  "timestamp": "2026-07-01T09:30:00Z",
  "data": {
    "subscriptionId": 12,
    "customerExternalId": "demo-user",
    "serviceCode": "office",
    "plan": "pro",
    "status": "active",
    "amount": 19900,
    "nextBillingDate": "2026-08-01",
    "ownerType": "organization",           // customer(개인) | organization(회사)
    "orgCode": "SH-ABCDEFGHIJ"             // 회사 소유 구독이면 조직코드(개인이면 없음) — 테넌트 그룹핑 키
  }
}
```

제품 수신부가 반드시 지켜야 할 것:
1. **서명 검증** — 요청 body(raw)를 공유 시크릿으로 HMAC-SHA256 hex 계산해
   `X-Synub-Signature`의 `sha256=` 뒤 값과 **상수시간 비교**. 불일치면 거부(위조 방지).
   시크릿 키 이름/값은 빌링 설정 `app.webhook.secret`(로컬 기본 `local-dev-webhook-secret`,
   운영은 env 주입)과 **제품 쪽에 동일하게** 공유돼야 한다.
2. **2xx 응답** — 성공 처리 시 HTTP 2xx를 반환할 것. 2xx가 아니면 빌링이 **최대 3회 지수 백오프 재시도**
   후 `failed`로 기록한다(`webhook_delivery` 감사 테이블). 웹훅 실패는 결제를 롤백하지 않는다(장애 격리).
3. **멱등 처리** — 재시도로 같은 이벤트가 2번 올 수 있다. `subscriptionId`+`event`+`timestamp` 기준으로
   중복 무시하도록 구현.
4. **status를 신뢰하되, 애매하면 ①로 재확인** — 웹훅은 알림, 최종 권한은 entitlements API가 확정.

> 웹훅은 **비동기(@Async)**로 나가며, `webhook_url`이 비어 있으면 발송 자체가 `failed`로 남는다.
> 따라서 §1.2에서 제품의 `webhook_url`을 반드시 채워야 이 연계가 동작한다.

---

### ③ 온보딩 핸드오프 — 구독 직후 제품 초기설정으로 이동 (범용)

셋업이 필요한 제품(그룹웨어 등)은 결제 완료 후 **"설정하러 가기"**로 제품 온보딩 페이지에 진입한다.
빌링은 이때 **변조 방지 서명 링크**를 발급한다. **특정 제품에 종속되지 않는 범용 API** — `service_code` +
`product.onboarding_url`로만 동작하므로(하드코딩 없음), 새 제품도 `onboarding_url`만 채우면 그대로 쓴다.

```
GET /organizations/{orgId}/handoff?service={serviceCode}
Authorization: (로그인 사용자 세션 — 해당 조직의 owner/billing_manager만)

→ 200 { "url": "https://office.synub.io/onboarding?customer=usr_...&company=우리회사&orgCode=SH-...&sig=sha256=..." }
```

발급 조건(모두 충족해야 함):
- 호출자가 그 조직의 **owner/billing_manager**(`requireManager`). member는 거부.
- 조직이 **인증 완료**(`verified` + `org_code` 존재). 미인증 조직은 `400`(결제·초기설정 차단 규칙).
- 제품에 **`onboarding_url`이 등록**돼 있어야 함. 없으면 `400`("초기설정 온보딩을 제공하지 않습니다").

서명 규약(웹훅과 동일):
- 페이로드 = `"{customerExternalId}|{orgCode}"`, 키 = `app.webhook.secret`(로컬 `local-dev-webhook-secret`).
- 반환 URL 쿼리에 `customer`·`company`(조직명)·`orgCode`·`sig`(`sha256=hex`)가 실린다.
- **제품 온보딩 수신부는 `customer|orgCode`를 같은 시크릿으로 재서명해 `sig`와 상수시간 비교**해야 한다(위조 차단).
  검증 통과 시 그 `orgCode`로 워크스페이스를 생성/바인딩한다. (office 예: `OnboardingService.validLink(customer, orgCode, sig)`)

> 이 링크는 **신원 증명이 아니라 조직 셋업 진입권**이다. 실제 로그인은 SSO로 별도 확립하고, 핸드오프는
> "이 사람이 이 조직 셋업을 시작하도록" 서명으로 보증하는 용도. 온보딩 완료 후에는 §7 신원 규약대로 SSO 로그인으로 운영.

---

### 연계 구현 체크리스트 (제품 서비스 쪽)

- [ ] 빌링에 `service_code`·`webhook_url` 등록 (이 문서 §1). 셋업형이면 `org_only`·`onboarding_url`도.
- [ ] `POST {webhook_url}` 수신 엔드포인트 구현 (서명검증 + 멱등 + 2xx)
- [ ] 통합 시크릿(`app.webhook.secret`) 공유
- [ ] 기능 접근 시 `GET /api/entitlements?service={code}&customer={userId}`로 권한 게이팅
- [ ] 이벤트별 권한 부여/회수 정책 정의 (특히 `payment_failed`·`suspended`·`canceled` 처리)
- [ ] (셋업형) 온보딩 수신부 구현 — `customer|orgCode` 서명 검증 + `org_code`로 워크스페이스 바인딩 (§7 ③)

> 관련 코드: `web/EntitlementController.java`(조회 API), `service/EntitlementService.java`(판정 로직),
> `service/SubscriptionWebhooks.java`(이벤트·페이로드), `service/WebhookService.java`(서명·재시도),
> `web/PgWebhookController.java`(반대로 PG→빌링 수신 스텁). PRD는 `README.md` §6(제품 연동 계약).

---

## 8. 참고 파일 지도

| 관심사 | 파일 |
|--------|------|
| 스키마 정의 + 데모 시드 | `apps/api/src/main/resources/db/migration/V1__init.sql` (product/plan/customer DDL + 제품 4·플랜 시드) |
| org_code 도입 | `apps/api/.../db/migration/V2__org_code.sql` |
| onboarding_url 도입 | `apps/api/.../db/migration/V3__product_onboarding_url.sql` |
| company_id 제거 | `apps/api/.../db/migration/V4__drop_company_id.sql` |
| 엔티티 | `apps/api/.../domain/Product.java`, `Plan.java` |
| 읽기 API | `apps/api/.../web/CatalogController.java` (`GET /products`), `service/CatalogService.java` |
| 온보딩 핸드오프 | `apps/api/.../web/HandoffController.java` (`GET /organizations/{orgId}/handoff?service=`) |
| DTO 변환·사용량 | `apps/api/.../service/DtoMapper.java` |
| 프론트 카탈로그 | `apps/web/app/products/page.tsx`, `components/product-icon.tsx` |
| PRD | `README.md` (§2 "새 제품 추가가 코드 수정 없이 카탈로그 등록만으로") |

> PRD의 이상(理想)은 "코드 수정 없이 카탈로그 등록만으로 제품 추가"다. 현재 구현에서 **카탈로그 등록 = 마이그레이션 1개**이고,
> 아이콘/사용량은 표시 품질을 위한 선택적 보강이다. 백엔드 로직(구독·청구·웹훅)은 제품이 늘어도 **수정 불필요**하다.
