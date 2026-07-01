// Synub Billing API 클라이언트 — 백엔드(apps/api) REST 계약과 1:1.

import { getToken, clearToken } from "./token";
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
  subscribers: number;
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

async function http<T>(path: string, init?: RequestInit): Promise<T> {
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
  if (res.status === 401) {
    // 토큰 만료/무효 — 정리하고 로그인 유도
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

export interface ApiOrg {
  id: number;
  name: string;
  role: OrgRole;
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

export const api = {
  products: () => http<ApiProduct[]>("/products"),
  dashboard: () => http<ApiDashboard>("/dashboard"),
  organizations: () => http<ApiOrg[]>("/organizations"),
  createOrganization: (name: string) =>
    http<ApiOrg>("/organizations", {
      method: "POST",
      body: JSON.stringify({ name }),
    }),

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
  subscriptions: () => http<ApiSubscription[]>("/subscriptions"),
  payments: () => http<ApiPayment[]>("/payments"),
  cards: () => http<ApiCard[]>("/billing/keys"),

  createSubscription: (planId: number, billingKeyId: number, seats?: number) =>
    http<ApiSubscription>("/subscriptions", {
      method: "POST",
      body: JSON.stringify({ planId, billingKeyId, seats }),
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
