// Synub Billing API 클라이언트 — 백엔드(apps/api) REST 계약과 1:1.

import { getToken, clearToken, refreshAccessToken } from "./token";
import { getContextHeader } from "./context";

const BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export type Cycle = "monthly" | "yearly";
export type SubStatus = "active" | "past_due" | "suspended" | "canceled";
export type PayStatus = "paid" | "failed" | "refunded" | "pending";

export type PricingType = "flat" | "per_seat";

export interface ApiPlan {
  id: number;
  code: string;
  name: string;
  tagline: string | null;
  amount: number;
  cycle: Cycle;
  features: string[];
  highlight: boolean;
  pricingType: PricingType;
}

export interface ApiProduct {
  serviceCode: string;
  name: string;
  category: string | null;
  description: string | null;
  domain: string | null;
  demoUrl: string | null;
  subscribers: number;
  orgOnly: boolean;
  status: string; // active(구독가능) | coming_soon(준비중·티저) | inactive(숨김, 카탈로그 미노출)
  plans: ApiPlan[];
}

export interface ApiCard {
  id: number;
  company: string;
  last4: string;
  type: string;
  isPrimary: boolean;
  billedCount: number;
}

export interface ApiUsage {
  label: string;
  unit: string;
  used: number;
  limit: number | null;
}

export interface ApiSubscription {
  id: number;
  serviceCode: string;
  product: string;
  plan: string;
  amount: number;
  cycle: Cycle;
  status: SubStatus;
  startedAt: string;
  nextBillingDate: string;
  card: string;
  cancelAtPeriodEnd: boolean;
  monthsActive: number;
  usage: ApiUsage | null;
  pricingType: PricingType;
  unitAmount: number;
  seats: number;
  creditBalance: number;
  complimentary: boolean;
}

export interface ApiPayment {
  id: number;
  serviceCode: string;
  product: string;
  plan: string;
  amount: number;
  status: PayStatus;
  date: string;
  method: string;
  receiptNo: string;
}

export interface ApiSpendPoint {
  month: string;
  amount: number;
}

export interface ApiSummary {
  activeCount: number;
  monthlyTotal: number;
  savedByYearly: number;
  paidThisYear: number;
  nextBillingDate: string | null;
  nextBillingProduct: string | null;
}

export interface ApiDashboard {
  summary: ApiSummary;
  activeSubscriptions: ApiSubscription[];
  recentPayments: ApiPayment[];
  spendHistory: ApiSpendPoint[];
}

async function http<T>(
  path: string,
  init?: RequestInit,
  retried = false
): Promise<T> {
  const token = getToken();
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      "X-Synub-Context": getContextHeader(),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
    cache: "no-store",
  });
  if (res.status === 401 && !retried) {
    // 액세스 토큰 만료 추정 — 리프레시로 갱신 후 1회 재시도
    const refreshed = await refreshAccessToken();
    if (refreshed) return http<T>(path, init, true);
  }
  if (res.status === 401) {
    // 갱신 실패 — 정리하고 로그인 유도
    clearToken();
  }
  if (!res.ok) {
    let detail = `${res.status} ${res.statusText}`;
    try {
      const body = await res.json();
      if (body?.detail) detail = body.detail;
    } catch {
      /* noop */
    }
    throw new Error(detail);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export type OrgRole = "owner" | "billing_manager" | "member";

export type VerifyStatus = "pending" | "verified" | "rejected";

export interface ApiOrg {
  id: number;
  name: string;
  role: OrgRole;
  verifyStatus: VerifyStatus;
  orgCode: string | null;
}

export interface ApiAdminOrg {
  id: number;
  name: string;
  businessNo: string | null;
  orgCode: string | null;
  verifyStatus: VerifyStatus;
  rejectReason: string | null;
}

// 관리자 개인(결제 고객) 목록 행 — SSO 통합계정 매핑
export interface ApiAdminCustomer {
  id: number;
  externalId: string;
  email: string | null;
  phone: string | null;
  subscriptions: number;
  createdAt: string | null;
}

// 관리자 제품 메타(가격/플랜 제외 — 그건 마이그레이션 전용)
export interface ApiAdminProduct {
  id: number;
  serviceCode: string;
  name: string;
  category: string | null;
  description: string | null;
  domainUrl: string | null;
  demoUrl: string | null;
  webhookUrl: string | null;
  onboardingUrl: string | null;
  sortOrder: number;
  orgOnly: boolean;
  status: string;
  planCount: number;
}

export interface ProductMetaInput {
  serviceCode?: string; // 생성 시에만
  name: string;
  category?: string | null;
  description?: string | null;
  domainUrl?: string | null;
  demoUrl?: string | null;
  webhookUrl?: string | null;
  onboardingUrl?: string | null;
  sortOrder?: number;
  orgOnly?: boolean;
  status?: string;
}

export interface ApiMember {
  customerId: number;
  externalId: string;
  email: string;
  role: OrgRole;
}

export interface ApiInvitation {
  id: number;
  organizationId: number;
  organizationName: string | null;
  email: string;
  role: OrgRole;
  status: string;
}

export interface ApiAdminStats {
  activeSubscriptions: number;
  customers: number;
  organizations: number;
  monthlyRevenue: number;
  paidThisMonth: number;
}

export interface ApiAdminSubscription {
  id: number;
  customerEmail: string;
  ownerType: string;
  product: string;
  plan: string;
  status: SubStatus;
  amount: number;
  nextBillingDate: string;
}

export interface ApiAdminPayment {
  id: number;
  customerEmail: string;
  product: string;
  amount: number;
  status: PayStatus;
  date: string;
  receiptNo: string;
}

// 관리자 대시보드 차트 데이터
export interface ApiMonthPoint { month: string; amount: number; count: number }
export interface ApiNameValue { name: string; value: number }
export interface ApiAdminAnalytics {
  revenueTrend: ApiMonthPoint[];
  subsTrend: ApiMonthPoint[];
  subsByStatus: ApiNameValue[];
  revenueByProduct: ApiNameValue[];
  paymentsByStatus: ApiNameValue[];
  orgsByStatus: ApiNameValue[];
}

export interface ApiProfile {
  avatarUrl: string | null;
}

export const api = {
  products: () => http<ApiProduct[]>("/products"),
  dashboard: () => http<ApiDashboard>("/dashboard"),
  organizations: () => http<ApiOrg[]>("/organizations"),

  // 마이페이지 프로필(사진). avatarUrl은 <img src>로 쓰도록 절대 URL로 변환.
  getProfile: async (): Promise<ApiProfile> => {
    const r = await http<ApiProfile>("/me/profile");
    return { avatarUrl: r.avatarUrl ? BASE + r.avatarUrl : null };
  },
  uploadAvatar: async (file: File): Promise<ApiProfile> => {
    const fd = new FormData();
    fd.append("avatar", file);
    const token = getToken();
    const res = await fetch(`${BASE}/me/avatar`, {
      method: "POST",
      headers: {
        "X-Synub-Context": getContextHeader(),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: fd,
    });
    if (!res.ok) {
      let detail = `${res.status} ${res.statusText}`;
      try {
        const b = await res.json();
        if (b?.detail) detail = b.detail;
      } catch {
        /* noop */
      }
      throw new Error(detail);
    }
    const r = (await res.json()) as ApiProfile;
    return { avatarUrl: r.avatarUrl ? BASE + r.avatarUrl : null };
  },
  deleteAvatar: () => http<void>("/me/avatar", { method: "DELETE" }),
  // 회사 생성 + 사업자 인증 서류(멀티파트). Content-Type 은 브라우저가 boundary 포함해 설정.
  createOrganization: async (input: {
    name: string;
    businessNo: string;
    repName: string;
    openDate: string;
    corpType: "corp" | "individual";
    corpNo?: string;
    document: File;
    identityVerificationId?: string;
  }) => {
    const fd = new FormData();
    fd.append("name", input.name);
    fd.append("businessNo", input.businessNo);
    fd.append("repName", input.repName);
    fd.append("openDate", input.openDate);
    fd.append("corpType", input.corpType);
    if (input.corpNo) fd.append("corpNo", input.corpNo);
    fd.append("document", input.document);
    if (input.identityVerificationId)
      fd.append("identityVerificationId", input.identityVerificationId);
    const token = getToken();
    const res = await fetch(`${BASE}/organizations`, {
      method: "POST",
      headers: {
        "X-Synub-Context": getContextHeader(),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: fd,
    });
    if (!res.ok) {
      let detail = `${res.status} ${res.statusText}`;
      try {
        const b = await res.json();
        if (b?.detail) detail = b.detail;
      } catch {
        /* noop */
      }
      throw new Error(detail);
    }
    return res.json() as Promise<ApiOrg>;
  },

  // 조직 멤버
  members: (orgId: number) => http<ApiMember[]>(`/organizations/${orgId}/members`),
  changeMemberRole: (orgId: number, customerId: number, role: OrgRole) =>
    http<void>(`/organizations/${orgId}/members/${customerId}`, {
      method: "PATCH",
      body: JSON.stringify({ role }),
    }),
  removeMember: (orgId: number, customerId: number) =>
    http<void>(`/organizations/${orgId}/members/${customerId}`, { method: "DELETE" }),

  // 초대 (조직 관리자)
  orgInvitations: (orgId: number) =>
    http<ApiInvitation[]>(`/organizations/${orgId}/invitations`),
  invite: (orgId: number, email: string, role: OrgRole) =>
    http<ApiInvitation>(`/organizations/${orgId}/invitations`, {
      method: "POST",
      body: JSON.stringify({ email, role }),
    }),
  cancelInvitation: (orgId: number, invitationId: number) =>
    http<void>(`/organizations/${orgId}/invitations/${invitationId}`, { method: "DELETE" }),

  // 초대 (받는 사람)
  myInvitations: () => http<ApiInvitation[]>("/invitations"),
  acceptInvitation: (id: number) =>
    http<void>(`/invitations/${id}/accept`, { method: "POST" }),
  declineInvitation: (id: number) =>
    http<void>(`/invitations/${id}/decline`, { method: "POST" }),

  // 관리자 콘솔
  adminStats: () => http<ApiAdminStats>("/admin/stats"),
  adminAnalytics: () => http<ApiAdminAnalytics>("/admin/analytics"),
  adminSubscriptions: () => http<ApiAdminSubscription[]>("/admin/subscriptions"),
  adminPayments: () => http<ApiAdminPayment[]>("/admin/payments"),
  adminRefund: (id: number) =>
    http<ApiAdminPayment>(`/admin/payments/${id}/refund`, { method: "POST" }),
  adminCustomers: () => http<ApiAdminCustomer[]>("/admin/customers"),
  adminOrganizations: () => http<ApiAdminOrg[]>("/admin/organizations"),
  adminApproveOrg: (id: number) =>
    http<void>(`/admin/organizations/${id}/approve`, { method: "POST" }),
  adminRejectOrg: (id: number, reason: string) =>
    http<void>(`/admin/organizations/${id}/reject`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    }),
  // 제품 메타 관리(가격/플랜은 마이그레이션 전용)
  adminProducts: () => http<ApiAdminProduct[]>("/admin/products"),
  adminCreateProduct: (req: ProductMetaInput) =>
    http<ApiAdminProduct>("/admin/products", {
      method: "POST",
      body: JSON.stringify(req),
    }),
  adminUpdateProduct: (id: number, req: ProductMetaInput) =>
    http<ApiAdminProduct>(`/admin/products/${id}`, {
      method: "PUT",
      body: JSON.stringify(req),
    }),
  // 사업자등록증 서류를 토큰 인증으로 받아 blob URL 생성(새 탭 열람용)
  adminOrgDocumentUrl: async (id: number) => {
    const token = getToken();
    const res = await fetch(`${BASE}/admin/organizations/${id}/document`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!res.ok) throw new Error("서류를 불러올 수 없습니다.");
    return URL.createObjectURL(await res.blob());
  },
  subscriptions: () => http<ApiSubscription[]>("/subscriptions"),
  payments: () => http<ApiPayment[]>("/payments"),
  cards: () => http<ApiCard[]>("/billing/keys"),

  createSubscription: (
    planId: number,
    billingKeyId: number,
    seats?: number,
    idempotencyKey?: string
  ) =>
    http<ApiSubscription>("/subscriptions", {
      method: "POST",
      body: JSON.stringify({ planId, billingKeyId, seats }),
      headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : {},
    }),
  cancelSubscription: (id: number) =>
    http<ApiSubscription>(`/subscriptions/${id}/cancel`, { method: "POST" }),
  changeSeats: (id: number, seats: number) =>
    http<ApiSubscription>(`/subscriptions/${id}/seats`, {
      method: "POST",
      body: JSON.stringify({ seats }),
    }),
  changePlan: (id: number, planId: number) =>
    http<ApiSubscription>(`/subscriptions/${id}/change-plan`, {
      method: "POST",
      body: JSON.stringify({ planId }),
    }),

  registerCard: (body: {
    pgBillingKey: string;
    cardCompany?: string;
    cardLast4?: string;
    cardType?: string;
    phone?: string;
    primary?: boolean;
  }) =>
    http<ApiCard>("/billing/keys", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  deleteCard: (id: number) =>
    http<void>(`/billing/keys/${id}`, { method: "DELETE" }),
  setPrimaryCard: (id: number) =>
    http<void>(`/billing/keys/${id}/primary`, { method: "POST" }),
};
