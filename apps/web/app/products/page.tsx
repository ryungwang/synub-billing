"use client";

import * as React from "react";
import { Check, Users, Sparkles, Loader2 } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  CheckoutDialog,
  type CheckoutTarget,
} from "@/components/checkout-dialog";
import { ProductIcon } from "@/components/product-icon";
import { api, type ApiProduct, type ApiPlan } from "@/lib/api";
import { cn, formatKRW } from "@/lib/utils";

export default function ProductsPage() {
  const [products, setProducts] = React.useState<ApiProduct[] | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [yearly, setYearly] = React.useState(false);
  const [target, setTarget] = React.useState<CheckoutTarget | null>(null);
  const [open, setOpen] = React.useState(false);

  React.useEffect(() => {
    api.products().then(setProducts).catch((e) => setError(e.message));
  }, []);

  function subscribe(product: ApiProduct, plan: ApiPlan) {
    setTarget({
      planId: plan.id,
      product: product.name,
      plan: plan.name,
      amount: plan.amount,
      cycle: plan.cycle,
    });
    setOpen(true);
  }

  return (
    <>
      <PageHeader
        title="제품 둘러보기"
        description="신업의 SaaS 제품을 하나의 계정으로 구독하세요."
        action={
          <div className="inline-flex items-center rounded-full border border-border bg-card p-1">
            <button
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
        }
      />

      {error && (
        <div className="rounded-2xl bg-destructive-subtle px-5 py-4 text-sm font-medium text-destructive-subtle-foreground">
          제품을 불러오지 못했습니다: {error}
        </div>
      )}

      {!products && !error && (
        <div className="flex items-center justify-center gap-2 py-24 text-muted-foreground">
          <Loader2 className="size-5 animate-spin" />
          불러오는 중…
        </div>
      )}

      <div className="space-y-10">
        {products?.map((product) => (
          <section key={product.serviceCode}>
            <div className="mb-4 flex items-start gap-3.5">
              <ProductIcon name={product.name} size="lg" />
              <div className="flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="text-lg font-extrabold tracking-tight">
                    {product.name}
                  </h2>
                  {product.category && (
                    <Badge variant="outline">{product.category}</Badge>
                  )}
                </div>
                <p className="mt-0.5 max-w-2xl text-sm text-muted-foreground">
                  {product.description}
                </p>
              </div>
              <div className="hidden shrink-0 items-center gap-1.5 text-xs font-medium text-muted-foreground sm:flex">
                <Users className="size-3.5" />
                {product.subscribers.toLocaleString()}명 이용 중
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {product.plans.map((plan) => {
                const isMonthlyPlan = plan.cycle === "monthly";
                const showYearly = yearly && isMonthlyPlan;
                const displayAmount = showYearly ? plan.amount * 10 : plan.amount;
                const cycleLabel =
                  showYearly || plan.cycle === "yearly" ? "년" : "월";

                return (
                  <Card
                    key={plan.id}
                    className={cn(
                      "relative flex flex-col p-6 transition-all hover:shadow-[var(--shadow-card-hover)]",
                      plan.highlight &&
                        "border-primary/40 ring-1 ring-primary/30"
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
                    <p className="mt-0.5 text-[13px] text-muted-foreground">
                      {plan.tagline}
                    </p>

                    <div className="mt-4">
                      {showYearly && (
                        <div className="text-sm text-muted-foreground line-through tnum">
                          {formatKRW(plan.amount * 12)}
                        </div>
                      )}
                      <div className="flex items-baseline gap-1">
                        <span className="text-[28px] font-extrabold tracking-tight tnum">
                          {formatKRW(displayAmount)}
                        </span>
                        <span className="text-sm font-medium text-muted-foreground">
                          / {cycleLabel}
                        </span>
                      </div>
                      {showYearly && (
                        <div className="mt-1">
                          <Badge variant="success">2개월 무료</Badge>
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

                    <Button
                      className="mt-6 w-full"
                      size="lg"
                      variant={plan.highlight ? "primary" : "outline"}
                      onClick={() => subscribe(product, plan)}
                    >
                      구독하기
                    </Button>
                  </Card>
                );
              })}
            </div>
          </section>
        ))}
      </div>

      <CheckoutDialog target={target} open={open} onOpenChange={setOpen} />
    </>
  );
}
