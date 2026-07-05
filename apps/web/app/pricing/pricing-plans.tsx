"use client";

import * as React from "react";
import { Sparkles, Check } from "lucide-react";
import { cn } from "@/lib/utils";

// /products API 응답 형태(요금 페이지가 서버에서 받아 그대로 전달). 제품 둘러보기와 동일 소스.
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

function won(n: number) {
  return "₩" + n.toLocaleString("ko-KR");
}

/**
 * 요금 카드 그리드 — 제품 둘러보기(/products)와 동일한 표현으로 통일.
 * 월간/연간 토글로 연간 플랜을 접고(별도 카드로 나열하지 않음), 인기 플랜을 강조한다.
 * 공개 카탈로그라 구독 버튼은 없고 read-only.
 */
export function PricingPlans({ products }: { products: Product[] }) {
  const [yearly, setYearly] = React.useState(false);
  const anyYearly = products.some((p) =>
    p.plans.some((pl) => pl.cycle === "yearly")
  );

  return (
    <>
      {anyYearly && (
        <div className="mb-8 flex justify-end">
          <div className="inline-flex items-center rounded-full border border-border bg-card p-1">
            <button
              type="button"
              onClick={() => setYearly(false)}
              className={cn(
                "rounded-full px-4 py-1.5 text-sm font-semibold transition-all",
                !yearly
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              월간
            </button>
            <button
              type="button"
              onClick={() => setYearly(true)}
              className={cn(
                "flex items-center gap-1.5 rounded-full px-4 py-1.5 text-sm font-semibold transition-all",
                yearly
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              연간
              <span
                className={cn(
                  "rounded-full px-1.5 py-0.5 text-[10px] font-bold",
                  yearly
                    ? "bg-primary-foreground/20 text-primary-foreground"
                    : "bg-success-subtle text-success-foreground"
                )}
              >
                -17%
              </span>
            </button>
          </div>
        </div>
      )}

      <div className="space-y-12">
        {products.map((product) => (
          <section key={product.serviceCode}>
            <div className="mb-4 flex flex-wrap items-center gap-2">
              <h2 className="text-lg font-extrabold tracking-tight">
                {product.name}
              </h2>
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
              <p className="mb-4 max-w-2xl text-sm text-muted-foreground">
                {product.description}
              </p>
            )}

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {product.plans
                .filter((p) => p.cycle === "monthly")
                .map((plan) => {
                  const perSeat = plan.pricingType === "per_seat";
                  // 같은 티어의 연간 플랜(코드 규약: <월간코드>_yearly). 있으면 토글로 연간가격 표시.
                  const yearlyPlan = product.plans.find(
                    (p) =>
                      p.cycle === "yearly" &&
                      p.code === (plan.code ?? "") + "_yearly"
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
          </section>
        ))}
      </div>
    </>
  );
}
