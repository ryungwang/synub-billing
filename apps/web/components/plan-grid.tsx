import * as React from "react";
import { Sparkles, Check } from "lucide-react";
import { cn } from "@/lib/utils";

// 카탈로그(/products) 응답 중 요금 카드 렌더에 필요한 최소 형태. 요금 페이지·제품 상세가 공유한다.
export type CatalogPlan = {
  id: number;
  code?: string;
  name: string;
  tagline: string | null;
  amount: number;
  cycle: "monthly" | "yearly";
  features: string[];
  highlight: boolean;
  pricingType: "flat" | "per_seat";
};
export type CatalogProduct = {
  serviceCode: string;
  name: string;
  category: string | null;
  description: string | null;
  orgOnly: boolean;
  status: string;
  demoUrl?: string | null;
  subscribers?: number;
  plans: CatalogPlan[];
};

function won(n: number) {
  return "₩" + n.toLocaleString("ko-KR");
}

/**
 * 읽기 전용 요금 카드 그리드(구독 버튼 없음) — 공개 요금 페이지·제품 상세가 함께 쓴다.
 * 월간 카드만 노출하고, 같은 티어의 연간 플랜(<코드>_yearly)이 있으면 yearly=true일 때 연간가로 전환.
 * 토글 상태는 부모가 소유(BillingToggle)한다.
 */
export function PlanGrid({
  product,
  yearly,
}: {
  product: CatalogProduct;
  yearly: boolean;
}) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {product.plans
        .filter((p) => p.cycle === "monthly")
        .map((plan) => {
          const perSeat = plan.pricingType === "per_seat";
          const yearlyPlan = product.plans.find(
            (p) => p.cycle === "yearly" && p.code === (plan.code ?? "") + "_yearly"
          );
          const useYear = yearly && !!yearlyPlan && !perSeat;
          const active = useYear && yearlyPlan ? yearlyPlan : plan;
          const cycleLabel = perSeat
            ? "인 · 월"
            : active.cycle === "yearly"
            ? "년"
            : "월";

          return (
            <div
              key={plan.id}
              className={cn(
                "relative flex flex-col rounded-2xl border border-border bg-card p-6",
                plan.highlight && "border-primary/40 ring-1 ring-primary/30"
              )}
            >
              {plan.highlight && (
                <div className="absolute -top-3 left-6">
                  <span className="inline-flex items-center gap-1 rounded-full bg-primary px-2.5 py-1 text-[11px] font-bold text-primary-foreground shadow-sm">
                    <Sparkles className="size-3" />
                    가장 인기
                  </span>
                </div>
              )}

              <span className="text-base font-bold">{plan.name}</span>
              {plan.tagline && (
                <p className="mt-0.5 text-[13px] text-muted-foreground">
                  {plan.tagline}
                </p>
              )}

              <div className="mt-4">
                {useYear && (
                  <div className="text-sm text-muted-foreground line-through tabular-nums">
                    {won(plan.amount * 12)}
                  </div>
                )}
                <div className="flex items-baseline gap-1">
                  <span className="text-[26px] font-extrabold tracking-tight tabular-nums">
                    {won(active.amount)}
                  </span>
                  <span className="text-sm font-medium text-muted-foreground">
                    / {cycleLabel}
                  </span>
                </div>
                {useYear && (
                  <div className="mt-1">
                    <span className="inline-flex rounded-full bg-success-subtle px-2 py-0.5 text-[11px] font-bold text-success-foreground">
                      2개월 무료
                    </span>
                  </div>
                )}
              </div>

              <ul className="mt-5 flex-1 space-y-2.5">
                {plan.features.map((f) => (
                  <li key={f} className="flex items-start gap-2 text-sm">
                    <Check className="mt-0.5 size-4 shrink-0 text-primary" />
                    <span className="text-secondary-foreground">{f}</span>
                  </li>
                ))}
              </ul>
            </div>
          );
        })}
    </div>
  );
}
