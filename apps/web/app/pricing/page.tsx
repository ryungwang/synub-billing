import Link from "next/link";
import type { Metadata } from "next";
import { COMPANY } from "@/lib/company";
import { PricingPlans } from "./pricing-plans";

// 항상 서버에서 최신 요금을 렌더(HTML에 가격 포함) — PG 심사 '상품 등록' 확인 + 검색 노출.
export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "요금 안내 — Synub Billing",
  description: "Synub Inc.의 SaaS 제품 구독 요금제와 가격을 확인하세요.",
};

type Plan = {
  id: number;
  name: string;
  code?: string;
  tagline: string | null;
  amount: number;
  cycle: "monthly" | "yearly";
  features: string[];
  highlight: boolean;
  pricingType: "flat" | "per_seat";
};
type Product = {
  serviceCode: string;
  name: string;
  category: string | null;
  description: string | null;
  orgOnly: boolean;
  status: string;
  plans: Plan[];
};

async function getProducts(): Promise<Product[]> {
  const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  try {
    const res = await fetch(`${base}/products`, { cache: "no-store" });
    if (!res.ok) return [];
    return (await res.json()) as Product[];
  } catch {
    return [];
  }
}

export default async function PricingPage() {
  const products = await getProducts();

  return (
    <div>
      <header className="mb-10">
        <h1 className="text-2xl font-extrabold tracking-tight">요금 안내</h1>
        <p className="mt-1.5 text-muted-foreground">
          {COMPANY.serviceName} — Synub Inc.의 여러 SaaS 제품을 하나의 계정으로 구독하고 결제하세요. 모든 요금은 부가세(VAT) 포함 원화(KRW) 기준입니다.
        </p>
      </header>

      {products.length === 0 ? (
        <p className="rounded-2xl border border-border bg-card px-5 py-4 text-sm text-muted-foreground">
          현재 표시할 요금제가 없습니다.
        </p>
      ) : (
        <PricingPlans products={products} />
      )}

      <div className="mt-12 rounded-2xl border border-border bg-card px-6 py-5 text-sm text-muted-foreground">
        구독하려면{" "}
        <Link href="/" className="font-bold text-primary hover:underline">
          로그인
        </Link>{" "}
        후 제품을 선택하세요. 구독은 매 결제주기 자동 갱신되며 언제든 해지할 수 있습니다. 해지·환불 규정은{" "}
        <Link href="/refund" className="font-semibold hover:underline">
          환불·청약철회 정책
        </Link>
        을 확인하세요.
      </div>
    </div>
  );
}
