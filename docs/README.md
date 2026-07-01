# synub-billing 문서

성격별로 폴더를 나눈다. **어느 폴더를 볼지**부터 고르면 된다.

## 📁 integration/ — 타 프로젝트(서비스) 에이전트 참고용
다른 서비스 repo(예: OOffice, postflow)가 **빌링에 제품을 등록하고 빌링과 연동**할 때 보는 계약 문서.
서비스 쪽에서 무엇을 구현해야 하는지가 여기 있다.

- [`integration/PRODUCT_REGISTRATION.md`](integration/PRODUCT_REGISTRATION.md) — 제품/요금제 카탈로그 **등록 방법**(마이그레이션) + **빌링↔서비스 연계**(entitlements 조회 API, 웹훅 수신, 서명검증)

## 📁 architecture/ — 빌링 내부 설계·의사결정
빌링 자체의 **아키텍처 결정과 청사진**. 구현 방향·확정된 모델이 여기 있다.

- [`architecture/IMPLEMENTATION.md`](architecture/IMPLEMENTATION.md) — **구현 현황(as-built)**: 시스템 구성·신원·조직·과금·운영·데이터모델·엔드포인트·실행법. **"지금 뭐가 돼 있나"는 여기부터.**
- [`architecture/IDENTITY_AND_ORG_BILLING.md`](architecture/IDENTITY_AND_ORG_BILLING.md) — 통합계정(SSO) · 조직 · 역할 · **개인/회사 컨텍스트 전환(Option 1 확정)** 신원 모델(설계 근거)

---

### 폴더 배치 기준
| 폴더 | 성격 | 주 독자 |
|------|------|---------|
| `integration/` | 연동·등록 계약 (외부에 노출되는 인터페이스) | **타 서비스 프로젝트 에이전트** |
| `architecture/` | 내부 설계·의사결정 (빌링을 어떻게 만드는가) | 빌링 repo 개발자/에이전트 |

> 새 문서를 추가할 때: **다른 프로젝트가 빌링과 맞물리려고 보는 것 → `integration/`**, **빌링을 어떻게 구현/설계하는가 → `architecture/`**.
