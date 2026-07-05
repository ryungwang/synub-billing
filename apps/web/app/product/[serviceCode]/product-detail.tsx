"use client";

import * as React from "react";
import Link from "next/link";
import { ArrowLeft, PlayCircle, Users, Check, Sparkles } from "lucide-react";
import { ProductIcon } from "@/components/product-icon";
import { BillingToggle } from "@/components/billing-toggle";
import { PlanGrid, type CatalogProduct } from "@/components/plan-grid";

/** 공개 제품 상세 — 히어로 + 주요 기능(플랜 features 종합) + 요금제(월/연 토글) + 로그인 CTA. */
export function ProductDetail({ product }: { product: CatalogProduct }) {
  const [yearly, setYearly] = React.useState(false);
  const anyYearly = product.plans.some((p) => p.cycle === "yearly");
  const comingSoon = product.status === "coming_soon";

  // 주요 기능 = 전 플랜 features 합집합(중복 제거, 등장 순서 유지). 실데이터라 과장 없음.
  const highlights = React.useMemo(() => {
    const seen = new Set<string>();
    const out: string[] = [];
    for (const pl of product.plans) {
      for (const f of pl.features) {
        if (!seen.has(f)) {
          seen.add(f);
          out.push(f);
        }
      }
    }
    return out;
  }, [product.plans]);

  return (
    <div>
      <Link
        href="/pricing"
        className="mb-6 inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        요금·제품
      </Link>

      {/* 히어로 */}
      <header className="flex flex-col gap-5 border-b border-border pb-9 sm:flex-row sm:items-start sm:gap-6">
        <ProductIcon name={product.name} size="xl" className="rounded-3xl" />
        <div className="flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-extrabold tracking-tight">
              {product.name}
            </h1>
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
            {comingSoon && (
              <span className="inline-flex items-center gap-1 rounded-full border border-primary/40 px-2 py-0.5 text-xs font-bold text-primary">
                <Sparkles className="size-3" /> 곧 출시
              </span>
            )}
          </div>
          {product.description && (
            <p className="mt-2 max-w-2xl text-[15px] leading-relaxed text-muted-foreground">
              {product.description}
            </p>
          )}
          <div className="mt-5 flex flex-wrap items-center gap-3">
            <Link
              href="/"
              className="inline-flex items-center rounded-full bg-primary px-5 py-2.5 text-sm font-bold text-primary-foreground transition-opacity hover:opacity-90"
            >
              {comingSoon ? "출시 알림 받기" : "구독하러 가기"}
            </Link>
            {product.demoUrl && (
              <a
                href={product.demoUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 rounded-full border border-primary/40 px-4 py-2.5 text-sm font-semibold text-primary transition-colors hover:bg-primary-subtle"
              >
                <PlayCircle className="size-4" />
                데모 체험하기
              </a>
            )}
            {product.subscribers != null && product.subscribers > 0 && (
              <span className="inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground">
                <Users className="size-4" />
                {product.subscribers.toLocaleString()}명 이용 중
              </span>
            )}
          </div>
        </div>
      </header>

      {/* 주요 기능 */}
      {highlights.length > 0 && (
        <section className="border-b border-border py-9">
          <h2 className="text-lg font-extrabold tracking-tight">주요 기능</h2>
          <ul className="mt-5 grid gap-x-8 gap-y-3 sm:grid-cols-2">
            {highlights.map((f) => (
              <li key={f} className="flex items-start gap-2.5 text-sm">
                <span className="mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-full bg-primary-subtle">
                  <Check className="size-3.5 text-primary" strokeWidth={3} />
                </span>
                <span className="text-secondary-foreground">{f}</span>
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* 요금제 */}
      <section className="py-9">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-extrabold tracking-tight">요금제</h2>
          {anyYearly && <BillingToggle yearly={yearly} onYearly={setYearly} />}
        </div>
        <PlanGrid product={product} yearly={yearly} />
      </section>

      {/* CTA */}
      <div className="rounded-2xl border border-border bg-card px-6 py-5 text-sm text-muted-foreground">
        구독하려면{" "}
        <Link href="/" className="font-bold text-primary hover:underline">
          로그인
        </Link>{" "}
        후 이 제품을 선택하세요.
        {product.orgOnly
          ? " 이 제품은 회사(조직) 계정에서 구독할 수 있어요."
          : " 구독은 매 결제주기 자동 갱신되며 언제든 해지할 수 있습니다."}{" "}
        해지·환불 규정은{" "}
        <Link href="/refund" className="font-semibold hover:underline">
          환불·청약철회 정책
        </Link>
        을 확인하세요.
      </div>
    </div>
  );
}
