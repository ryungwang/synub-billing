// 포트원 V2 브라우저 SDK — 카드 빌링키 발급 (정기결제용).
// storeId/channelKey 가 설정돼 있으면 실제 결제창을 띄우고, 없으면 null 반환(데모 폴백).

const STORE_ID = process.env.NEXT_PUBLIC_PORTONE_STORE_ID;
const CHANNEL_KEY = process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY;
const IDENTITY_CHANNEL_KEY = process.env.NEXT_PUBLIC_PORTONE_IDENTITY_CHANNEL_KEY;

export const portoneConfigured = Boolean(STORE_ID && CHANNEL_KEY);
export const identityConfigured = Boolean(STORE_ID && IDENTITY_CHANNEL_KEY);

/** 대표자 본인인증(통신사 PASS). 성공 시 identityVerificationId 반환(서버가 PortOne로 검증). */
export async function verifyRepresentative(): Promise<string | null> {
  if (!STORE_ID || !IDENTITY_CHANNEL_KEY) return null;
  const PortOne = (await import("@portone/browser-sdk/v2")).default;
  const id = `synub-idv-${Date.now()}`;
  const res = await PortOne.requestIdentityVerification({
    storeId: STORE_ID,
    identityVerificationId: id,
    channelKey: IDENTITY_CHANNEL_KEY,
  });
  if (!res || res.code != null) {
    throw new Error(res?.message ?? "본인인증에 실패했습니다.");
  }
  return res.identityVerificationId;
}

export interface IssuedBillingKey {
  billingKey: string;
}

export interface BillingCustomer {
  email: string;
  fullName?: string;
  phoneNumber?: string;
}

export async function issueBillingKey(
  customer: BillingCustomer
): Promise<IssuedBillingKey | null> {
  if (!STORE_ID || !CHANNEL_KEY) return null;

  const PortOne = (await import("@portone/browser-sdk/v2")).default;
  const issueId = `synub-bk-${Date.now()}`;

  // 포트원은 customer.email에 유효한 이메일 형식만 허용 — 통합계정 로그인ID가 이메일이 아닐 수 있으므로
  // 형식이 맞을 때만 전달(아니면 생략). 이름/전화도 값이 있을 때만.
  const isEmail = (v?: string) => !!v && /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v);
  const customerParam: { email?: string; fullName?: string; phoneNumber?: string } = {};
  if (isEmail(customer.email)) customerParam.email = customer.email;
  if (customer.fullName) customerParam.fullName = customer.fullName;
  if (customer.phoneNumber) customerParam.phoneNumber = customer.phoneNumber;

  const res = await PortOne.requestIssueBillingKey({
    storeId: STORE_ID,
    channelKey: CHANNEL_KEY,
    billingKeyMethod: "CARD",
    issueId,
    issueName: "Synub 정기결제 카드 등록",
    customer: customerParam,
  });

  if (!res || res.code != null) {
    throw new Error(res?.message ?? "빌링키 발급에 실패했습니다.");
  }
  return { billingKey: res.billingKey };
}
