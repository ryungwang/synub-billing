// 포트원 V2 브라우저 SDK — 카드 빌링키 발급 (정기결제용).
// storeId/channelKey 가 설정돼 있으면 실제 결제창을 띄우고, 없으면 null 반환(데모 폴백).

const STORE_ID = process.env.NEXT_PUBLIC_PORTONE_STORE_ID;
const CHANNEL_KEY = process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY;

export const portoneConfigured = Boolean(STORE_ID && CHANNEL_KEY);

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
  const res = await PortOne.requestIssueBillingKey({
    storeId: STORE_ID,
    channelKey: CHANNEL_KEY,
    billingKeyMethod: "CARD",
    issueId,
    issueName: "Synub 정기결제 카드 등록",
    // KG이니시스 V2 빌링키 발급은 구매자 이메일이 필수
    customer: {
      email: customer.email,
      fullName: customer.fullName,
      phoneNumber: customer.phoneNumber,
    },
  });

  if (!res || res.code != null) {
    throw new Error(res?.message ?? "빌링키 발급에 실패했습니다.");
  }
  return { billingKey: res.billingKey };
}
