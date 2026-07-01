# 구현 현황 (as-built)

이 문서는 **현재 실제로 구현·검증된 것**을 기술한다. 설계 의도는
[`IDENTITY_AND_ORG_BILLING.md`](IDENTITY_AND_ORG_BILLING.md)(신원·조직 설계),
연동 계약은 [`../integration/PRODUCT_REGISTRATION.md`](../integration/PRODUCT_REGISTRATION.md) 참조.

---

## 1. 시스템 구성

| 서비스 | repo | 로컬 포트 | DB | 역할 |
|--------|------|:--------:|----|------|
| **빌링** | `synub-billing` (`apps/api`) | 8088 (prod 8080) | `synub_billing` | 카탈로그·구독·결제·조직·과금·관리자 |
| **SSO(통합계정)** | `synub-sso` | 8090 | `synub_sso` | 신원·로그인·토큰 발급 |
| **웹** | `synub-billing` (`apps/web`) | 3100 | — | Next.js 15 프론트 |

- **DB**: 공용 로컬 Postgres 컨테이너 `postgres`(5432) 안에 `synub_billing` / `synub_sso`. 서비스는 서로의 DB를 직접 보지 않는다(토큰 + API로만 연동).
- **스택**: Spring Boot 3.3.5 / Java 21 / Flyway / JPA · Next.js 15 / Tailwind v4 · 디자인 = Toss 톤 단일 파랑.
- ⚠️ 빌링·SSO는 **별도 repo**지만 개발은 한 워크스페이스에서 절대경로로 함께 한다.

---

## 2. 신원 · 인증 (SSO ↔ 빌링)

**로그인의 진실 = SSO.** 빌링을 포함한 모든 서비스는 SSO가 발급한 **RS256 JWT**를 검증해 로그인을 확인한다(공유 DB 없음).

- **SSO 발급**: `POST /auth/login` → 액세스 토큰(RS256, 1h) + **리프레시 토큰**(14d). `iss=https://accounts.synub.io`, `aud=[synub-billing,...]`, `sub=external_id`, claims `email/name/admin`.
- **JWKS**: `GET /.well-known/jwks.json` — 공개키만. 빌링이 이걸로 서명 검증(`app.sso.mode=jwks`).
- **서명키 영속화**: SSO가 부팅 시 RSA 키를 DB(`signing_key`)에 저장·재사용 → **재기동해도 kid 유지, 기존 토큰 유효**. 운영은 `SSO_SIGNING_KEY`(env) 우선.
- **리프레시 토큰**: 원문 미저장(SHA-256 해시). `POST /auth/refresh`(회전=기존 폐기+신규), `POST /auth/logout`(폐기). **재사용 감지(theft detection)**: 폐기된 토큰 재사용 시 계정 전체 토큰 폐기. 만료/폐기분은 스케줄러가 정리.
- **빌링 검증 seam**(`io.synub.billing.auth`): `JwtTokenVerifier`(서명·iss·aud·exp), `IdentityFilter`(Bearer→`IdentityContext`, `X-Synub-Context` 파싱, CORS 프리플라이트 우회, 잘못된 토큰 401), `CurrentUser`(요청 신원 기반, JIT customer provisioning).
- **로그인 하드게이트**: 무토큰 401. 프론트 `AuthGate`가 로그인 전 전체화면 로그인. 데모는 SSO 데모 계정(`demo@synub.io`/`demo1234`, external_id=`demo-user`, **admin**)으로 원클릭 체험. 프론트는 액세스 만료 시 리프레시로 자동 갱신.

---

## 3. 조직 · 역할 · 컨텍스트

로그인은 1개(통합계정), 데이터는 **컨텍스트 + 역할**로 격리. 상세 설계는 [`IDENTITY_AND_ORG_BILLING.md`](IDENTITY_AND_ORG_BILLING.md).

- **조직/멤버십/역할**(`organization`/`membership`): 역할 `owner` / `billing_manager` / `member`. 조직 생성자는 owner.
- **사업자 인증(도용·마구잡이 방지)**: 회사 생성 시 ①사업자등록번호 형식·체크섬(+국세청 상태조회 API 훅, `BusinessVerifier`)로 **실존** 검증·중복 금지 ②**사업자등록증 서류 제출**(`StorageService`, 로컬 파일시스템/운영 S3) ③**관리자 심사(승인/반려)**로 **소유권** 확정. 생성 직후 `verify_status=pending`이며 **인증 완료 전 결제·구독 차단**(`CurrentScope.writeOwner`). 규칙: [[memory: org-verification-rule]].
- **개인/회사 컨텍스트**: 프론트 상단 전환기 → `X-Synub-Context: personal | org:{id}` 헤더. 빌링 `CurrentScope`가 소유 스코프(`Owner`)를 결정하고 조직이면 멤버십 검증(미소속 403).
- **소유 스코프 격리**: 구독·카드에 `owner_type/owner_id`(개인 customer / 조직 organization). 조회는 소유 스코프로만, 쓰기는 조직이면 결제관리 역할(owner/billing_manager) 필요. 개인 카드로 회사 구독 등 **교차 차단**.
- **멤버 초대**: 관리자가 이메일로 초대(pending) → **초대 이메일 발송** → 대상이 로그인(토큰 email 매칭) 후 앱 내 "받은 초대"에서 수락 시 멤버십 생성. owner의 역할 변경/제거(마지막 owner 보호).
- **entitlement**: `GET /api/entitlements?service=&customer=` — 개인 구독 **OR** 소속 조직 구독으로 접근 판정(제품이 호출).

---

## 4. 결제 · 구독

- **카탈로그**: product/plan (등록은 마이그레이션, [`../integration/PRODUCT_REGISTRATION.md`](../integration/PRODUCT_REGISTRATION.md)).
- **구독 생성/관리**: `POST /subscriptions`(+첫 결제, **Idempotency-Key**로 이중청구 방지), 해지·플랜변경·좌석변경.
- **PG 게이트웨이**: `PaymentGateway` 추상화. `MockPaymentGateway`(기본) / `PortOnePaymentGateway`(`app.portone.enabled=true`, 포트원 V2, **토스페이먼츠 채널**). `charge` + `refund`.
- **정기청구 상태머신**(`BillingEngine`): active⇄past_due→suspended, 재시도 D+1/3/5(`app.billing.retry-days`), past_due→(성공)→active.
- **웹훅 발신**(`WebhookService`): activated/canceled/payment_failed/suspended/plan_changed, HMAC-SHA256 서명 `X-Synub-Signature`, 재시도, `webhook_delivery` 감사.
- **결제 알림 이메일**: 실패(past_due)·정지(suspended)·정상화(recovered) 시 통지. 수신자 = 개인 구독은 본인, 조직 구독은 결제관리 멤버 전원.

---

## 5. 과금 (generic)

- **정액 / 인원당(seat)**: `plan.pricing_type = flat | per_seat`. per_seat이면 청구 = 단가 × 좌석. 특정 제품 하드코딩 아님.
- **비례 정산(proration)**: 좌석 증가 → 남은 기간 비례분 즉시 청구, 감소 → 비례분 **크레딧**(`subscription.credit_balance`) 적립 → 다음 청구에서 차감.
- **멱등키**(`idempotency_key`): `(scope, key)` 유니크. 동일 키 재요청은 재실행 없이 저장 응답 반환, 타 고객 키는 404. 7일 경과분 정리.

---

## 6. 운영 견고성 · 관리자

- **분산 스케줄러(ShedLock)**: 자동청구 크론이 다중 인스턴스에서 한 노드만 실행(`billing.shedlock`, `@SchedulerLock`).
- **관리자 콘솔**: SSO `account.is_admin` → JWT `admin` claim → 빌링 `CurrentUser.isAdmin`. `/admin/*`는 admin만(403). 통계(활성구독·MRR·이달결제·고객·조직)·전체 구독·최근 결제·**환불**. 프론트 `/admin` 페이지 + 사이드바 관리자 메뉴(admin만).
- **정리 스케줄러**: 만료/폐기 리프레시 토큰(SSO), 오래된 멱등키(빌링).
- **보안 견고성**: 공개경로 서비스인증(§8)·PG 웹훅 서명검증+금액대조 · SSO 로그인 무차별 대입 잠금(이메일 5회/15분) · 업로드 파일 매직바이트 검증 · 초대 7일 만료. 핵심 로직 단위 테스트(사업자 체크섬·웹훅 서명·서비스인증·레이트리밋).
- **단일 테넌트(의도)**: synub 자사 제품 전용이라 `company_id=1` 고정(멀티테넌트 SaaS 아님). 조직(회사)은 이 테넌트 안의 결제 주체 단위.

---

## 7. 데이터 모델 (Flyway)

배포 전 단계라 마이그레이션을 **프로젝트별 단일 베이스라인 `V1__init.sql`로 스쿼시**했다(스키마 + 전 테이블·주요 컬럼 `COMMENT` + 데모 시드). 이후 변경은 V2부터 증분 추가.

- **빌링 `synub_billing`(schema `billing`)** — `V1__init.sql`: product·plan·customer·billing_key·organization·membership·invitation·subscription·payment·usage_record·webhook_delivery·idempotency_key·shedlock + 데모 시드(제품4·플랜9·구독4·결제6).
- **SSO `synub_sso`(schema `sso`)** — `V1__init.sql`: account·signing_key·refresh_token (계정은 런타임 시더 Demo/Admin가 생성).

> 스키마는 DB `COMMENT`로 자기설명(테이블·핵심 컬럼 전부). `\d+ <table>`로 확인 가능.

---

## 8. 주요 엔드포인트

**SSO(8090)** — `POST /auth/{register,login,refresh,logout}`, `GET /.well-known/jwks.json`

**빌링(8088)**
| 그룹 | 엔드포인트 |
|------|-----------|
| 신원 | `GET /me` |
| 카탈로그·대시보드 | `GET /products`, `GET /dashboard` |
| 구독 | `GET/POST /subscriptions`, `POST /subscriptions/{id}/{cancel,change-plan,seats}` |
| 결제수단 | `GET/POST /billing/keys`, `DELETE /billing/keys/{id}`, `POST /billing/keys/{id}/primary` |
| 결제내역 | `GET /payments` |
| 조직 | `GET/POST /organizations`, `.../{id}/members`(GET/PATCH/DELETE), `.../{id}/invitations`(GET/POST/DELETE) |
| 초대(수신) | `GET /invitations`, `POST /invitations/{id}/{accept,decline}` |
| 관리자 | `GET /admin/{stats,subscriptions,payments,organizations}`, `POST /admin/payments/{id}/refund`, `POST /admin/organizations/{id}/{approve,reject}`, `GET /admin/organizations/{id}/document` |
| 제품 연동 | `GET /api/entitlements`(X-Service-Key), `POST /webhooks/portone`(서명검증), `POST /internal/billing/run`(X-Internal-Secret) |

> 🔐 공개경로 3종은 사용자 토큰이 아닌 **서비스 인증**: entitlements=`X-Service-Key`, internal=`X-Internal-Secret`, PortOne 웹훅=Standard Webhooks 서명검증(리플레이 방지). 시크릿 미설정 시 fail-closed(거부).

---

## 9. 로컬 실행

```bash
# DB: 공용 postgres 컨테이너(5432)는 상시 실행. synub_billing / synub_sso 사용.
# SSO
cd synub-sso && JAVA_HOME=<jdk21> ./gradlew bootRun          # :8090
# 빌링
cd synub-billing && JAVA_HOME=<jdk21> ./gradlew :apps:api:bootRun   # :8088
# 웹
cd synub-billing && PORT=3100 pnpm --filter web dev          # :3100
```
데모 로그인: 로그인 화면 "데모 계정으로 둘러보기"(demo@synub.io, admin).

> 남은 것: **배포**(프론트 Vercel / 백엔드·SSO Lightsail Docker+Caddy — 전역 인프라 규약).
