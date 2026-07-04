/**
 * 사업자·서비스 표시 정보 (전자상거래법 표시사항 + PG 심사용).
 * 한 곳에서 관리 — 푸터·이용약관·개인정보처리방침·환불정책이 모두 여기서 값을 읽는다.
 * 값이 바뀌면 이 파일만 고치면 된다.
 */
export const COMPANY = {
  // 법적 상호: 국문 등록상호 + 영문상호(영문 사업자등록증) 병기. 전자상거래법 표시란·PG 심사용.
  legalName: "주식회사 신업 (Synub Inc.)",
  enName: "Synub Inc.",
  serviceName: "Synub Billing",
  /** 대표자 — 전자상거래 표시는 사업자등록증(세무서) 기준 단독대표. */
  ceo: "김륜광",
  bizRegNo: "701-87-03590",
  /** 통신판매업 신고번호 (관할 전주완산). */
  mailOrderNo: "2026-전주완산-0504",
  /** 사업장 주소 — 통신판매업 신고 주소와 일치. */
  address: "전북특별자치도 전주시 완산구 삼천천변2길 36-16, 4층 404호",
  /** 고객센터 전화 — 필요 시 070 대표번호로 교체 권장. */
  tel: "010-2105-7767",
  email: "haru@synub.io",
  /** 개인정보 보호책임자. */
  privacyOfficer: "김륜광",
  privacyEmail: "haru@synub.io",
  homepage: "https://synub.io",
  appUrl: "https://app.synub.io",
} as const;
