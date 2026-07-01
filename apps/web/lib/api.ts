// Synub Billing API 클라이언트 — 백엔드(apps/api) REST 계약과 1:1.

import { getToken, clearToken } from "./token";
import { getContextHeader } from "./context";

const BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export type Cycle = "monthly" | "yearly";
export type SubStatus = "active" | "past_due" | "suspended" | "canceled";
export type PayStatus = "paid" | "failed" | "refunded" | "pending";

export interface ApiPlan {
  id: number;
  code: string;
  name: string;
  tagline: string | null;
  amount: number;
  cycle: Cycle;
  features: string[];
  highlight: boolean;
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

export interface ApiOrg {
  id: number;
  name: string;
  role: "owner" | "billing_manager" | "member";
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
  subscriptions: () => http<ApiSubscription[]>("/subscriptions"),
  payments: () => http<ApiPayment[]>("/payments"),
  cards: () => http<ApiCard[]>("/billing/keys"),

  createSubscription: (planId: number, billingKeyId: number) =>
    http<ApiSubscription>("/subscriptions", {
      method: "POST",
      body: JSON.stringify({ planId, billingKeyId }),
    }),
  cancelSubscription: (id: number) =>
    http<ApiSubscription>(`/subscriptions/${id}/cancel`, { method: "POST" }),
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
