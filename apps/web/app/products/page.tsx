"use client";

import * as React from "react";
import Link from "next/link";
import { Check, Users, Sparkles, Loader2, PlayCircle } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  CheckoutDialog,
  type CheckoutTarget,
} from "@/components/checkout-dialog";
import { ProductIcon } from "@/components/product-icon";
import { api, type ApiProduct, type ApiPlan, type ApiSubscription } from "@/lib/api";
import { rawContext, subscribeContext, contextOrgId } from "@/lib/context";
import { cn, formatKRW } from "@/lib/utils";

export default function ProductsPage() {
  const [products, setProducts] = React.useState<ApiProduct[] | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [yearly, setYearly] = React.useState(false);
  const [target, setTarget] = React.useState<CheckoutTarget | null>(null);
  const [open, setOpen] = React.useState(false);
  const [subs, setSubs] = React.useState<ApiSubscription[] | null>(null);
  const ctx = React.useSyncExternalStore(subscribeContext, rawContext, () => "personal");
  const isOrg = contextOrgId(ctx) !== null;

  React.useEffect(() => {
    api.products().then(setProducts).catch((e) => setError(e.message));
  }, []);

  // 내 구독 현황 — 현재 컨텍스트(개인/회사) 기준. 컨텍스트 바뀌면 다시 조회.
  React.useEffect(() => {
    api.subscriptions().then(setSubs).catch(() => setSubs([]));
  }, [ctx]);

  // serviceCode → 현재 컨텍스트에서 이용 중(active/past_due)인 구독.
  const mySubByService = React.useMemo(() => {
    const m = new Map<string, ApiSubscription>();
    for (const s of subs ?? []) {
      if (s.status !== "active" && s.status !== "past_due") continue;
      if (!m.has(s.serviceCode)) m.set(s.serviceCode, s);
    }
    return m;
  }, [subs]);

  function subscribe(product: ApiProduct, plan: ApiPlan) {
    setTarget({
      planId: plan.id,
      product: product.name,
      plan: plan.name,
      amount: plan.amount,
      cycle: plan.cycle,
      pricingType: plan.pricingType,
    });
    setOpen(true);
  }

  return (
    <>
      <PageHeader
        title="제품 둘러보기"
        description="Synub Inc.의 SaaS 제품을 하나의 계정으로 구독하세요."
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
        {products?.map((product) => {
          const mySub = mySubByService.get(product.serviceCode) ?? null;
          return (
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
                  {product.orgOnly && <Badge>조직 전용</Badge>}
                  {mySub && (
                    <Badge variant="success">
                      <Check className="size-3" />
                      구독 중{mySub.complimentary ? " · 무상" : ""}
                    </Badge>
                  )}
                  {product.status === "coming_soon" && (
                    <Badge variant="outline" className="border-primary/40 text-primary">
                      <Sparkles className="size-3" /> 곧 출시
                    </Badge>
                  )}
                </div>
                <p className="mt-0.5 max-w-2xl text-sm text-muted-foreground">
                  {product.description}
                </p>
              </div>
              <div className="flex shrink-0 flex-col items-end gap-2">
                {product.demoUrl && (
                  <a
                    href={product.demoUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1.5 rounded-full border border-primary/40 px-3 py-1.5 text-sm font-semibold text-primary transition-colors hover:bg-primary-subtle"
                  >
                    <PlayCircle className="size-4" />
                    데모 체험하기
                  </a>
                )}
                <div className="hidden items-center gap-1.5 text-xs font-medium text-muted-foreground sm:flex">
                  <Users className="size-3.5" />
                  {product.subscribers.toLocaleString()}명 이용 중
                </div>
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {product.plans
                .filter((p) => p.cycle === "monthly")
                .map((plan) => {
                const perSeat = plan.pricingType === "per_seat";
                // 같은 티어의 연간 플랜(코드 규약: <월간코드>_yearly). 있으면 토글로 연간가격·연간구독.
                const yearlyPlan = product.plans.find(
                  (p) => p.cycle === "yearly" && p.code === plan.code + "_yearly"
                );
                const useYear = yearly && !!yearlyPlan && !perSeat;
                const active = useYear && yearlyPlan ? yearlyPlan : plan;
                // 이 카드(티어)가 현재 이용 중인 플랜인지 — 월간/연간 이름 둘 다 대조.
                const isCurrent =
                  !!mySub &&
                  (mySub.plan === plan.name ||
                    (!!yearlyPlan && mySub.plan === yearlyPlan.name));
                const cycleLabel = perSeat
                  ? "인 · 월"
                  : active.cycle === "yearly"
                  ? "년"
                  : "월";

                return (
                  <Card
                    key={plan.id}
                    className={cn(
                      "relative flex flex-col p-6 transition-all hover:shadow-[var(--shadow-card-hover)]",
                      plan.highlight &&
                        "border-primary/40 ring-1 ring-primary/30",
                      isCurrent && "border-success/50 ring-1 ring-success/40"
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
                      {useYear && (
                        <div className="text-sm text-muted-foreground line-through tnum">
                          {formatKRW(plan.amount * 12)}
                        </div>
                      )}
                      <div className="flex items-baseline gap-1">
                        <span className="text-[28px] font-extrabold tracking-tight tnum">
                          {formatKRW(active.amount)}
                        </span>
                        <span className="text-sm font-medium text-muted-foreground">
                          / {cycleLabel}
                        </span>
                      </div>
                      {useYear && (
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

                    {product.status === "coming_soon" ? (
                      <Button
                        className="mt-6 w-full"
                        size="lg"
                        variant="outline"
                        disabled
                      >
                        출시 예정
                      </Button>
                    ) : isCurrent ? (
                      <Button
                        className="mt-6 w-full"
                        size="lg"
                        variant="outline"
                        disabled
                      >
                        <Check /> 이용 중
                      </Button>
                    ) : mySub ? (
                      <Button
                        className="mt-6 w-full"
                        size="lg"
                        variant="outline"
                        asChild
                      >
                        <Link href="/subscriptions">구독 관리</Link>
                      </Button>
                    ) : (
                      <Button
                        className="mt-6 w-full"
                        size="lg"
                        variant={plan.highlight ? "primary" : "outline"}
                        disabled={product.orgOnly && !isOrg}
                        onClick={() => subscribe(product, active)}
                      >
                        {product.orgOnly && !isOrg ? "회사 계정 전용" : "구독하기"}
                      </Button>
                    )}
                    {product.status === "coming_soon" ? (
                      <p className="mt-2 text-center text-[11px] text-muted-foreground">
                        출시 준비 중이에요 — 곧 만나요
                      </p>
                    ) : isCurrent ? (
                      <p className="mt-2 text-center text-[11px] text-muted-foreground">
                        <Link href="/subscriptions" className="hover:text-foreground">
                          구독 관리에서 플랜 변경·해지
                        </Link>
                      </p>
                    ) : product.orgOnly && !isOrg ? (
                      <p className="mt-2 text-center text-[11px] text-muted-foreground">
                        상단에서 회사로 전환하면 구독할 수 있어요
                      </p>
                    ) : null}
                  </Card>
                );
              })}
            </div>
          </section>
          );
        })}
      </div>

      <CheckoutDialog target={target} open={open} onOpenChange={setOpen} />
    </>
  );
}
