import Link from "next/link";
import type { Metadata } from "next";
import { COMPANY } from "@/lib/company";

// 항상 서버에서 최신 요금을 렌더(HTML에 가격 포함) — PG 심사 '상품 등록' 확인 + 검색 노출.
export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "요금 안내 — Synub Billing",
  description: "신업의 SaaS 제품 구독 요금제와 가격을 확인하세요.",
};

type Plan = {
  id: number;
  name: string;
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

function won(n: number) {
  return "₩" + n.toLocaleString("ko-KR");
}

function cycleLabel(plan: Plan) {
  if (plan.pricingType === "per_seat") return "인 · 월";
  return plan.cycle === "yearly" ? "년" : "월";
}

export default async function PricingPage() {
  const products = await getProducts();

  return (
    <div>
      <header className="mb-10">
        <h1 className="text-2xl font-extrabold tracking-tight">요금 안내</h1>
        <p className="mt-1.5 text-muted-foreground">
          {COMPANY.serviceName} — 신업의 여러 SaaS 제품을 하나의 계정으로 구독하고 결제하세요. 모든 요금은 부가세(VAT) 포함 원화(KRW) 기준입니다.
        </p>
      </header>

      {products.length === 0 && (
        <p className="rounded-2xl border border-border bg-card px-5 py-4 text-sm text-muted-foreground">
          현재 표시할 요금제가 없습니다.
        </p>
      )}

      <div className="space-y-12">
        {products.map((product) => (
          <section key={product.serviceCode}>
            <div className="mb-4 flex flex-wrap items-center gap-2">
              <h2 className="text-lg font-extrabold tracking-tight">{product.name}</h2>
              {product.category && (
                <span className="rounded-full border border-border px-2 py-0.5 text-xs font-medium text-muted-foreground">
                  {product.category}
                </span>
              )}
              {product.orgOnly && (
                <span className="rounded-full bg-primary px-2 py-0.5 text-xs font-bold text-primary-foreground">
                  조직 전용
                </span>
              )}
              {product.status === "coming_soon" && (
                <span className="rounded-full border border-primary/40 px-2 py-0.5 text-xs font-bold text-primary">
                  곧 출시
                </span>
              )}
            </div>
            {product.description && (
              <p className="mb-4 max-w-2xl text-sm text-muted-foreground">{product.description}</p>
            )}

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {product.plans.map((plan) => (
                <div
                  key={plan.id}
                  className="flex flex-col rounded-2xl border border-border bg-card p-6"
                >
                  <span className="text-base font-bold">{plan.name}</span>
                  {plan.tagline && (
                    <p className="mt-0.5 text-[13px] text-muted-foreground">{plan.tagline}</p>
                  )}
                  <div className="mt-4 flex items-baseline gap-1">
                    <span className="text-[26px] font-extrabold tracking-tight tabular-nums">
                      {won(plan.amount)}
                    </span>
                    <span className="text-sm font-medium text-muted-foreground">
                      / {cycleLabel(plan)}
                    </span>
                  </div>
                  <ul className="mt-5 flex-1 space-y-2 text-sm text-secondary-foreground">
                    {plan.features.map((f) => (
                      <li key={f}>· {f}</li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          </section>
        ))}
      </div>

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
