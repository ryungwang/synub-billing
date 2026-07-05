"use client";

import * as React from "react";
import Link from "next/link";
import { ChevronRight } from "lucide-react";
import { BillingToggle } from "@/components/billing-toggle";
import { PlanGrid, type CatalogProduct } from "@/components/plan-grid";

/**
 * 요금 카드 그리드 — 제품 둘러보기(/products)와 동일한 표현으로 통일.
 * 월간/연간 토글로 연간 플랜을 접고(별도 카드로 나열하지 않음), 인기 플랜을 강조한다.
 * 공개 카탈로그라 구독 버튼은 없고, 제품명은 공개 제품 상세(/product/{code})로 링크한다.
 */
export function PricingPlans({ products }: { products: CatalogProduct[] }) {
  const [yearly, setYearly] = React.useState(false);
  const anyYearly = products.some((p) =>
    p.plans.some((pl) => pl.cycle === "yearly")
  );

  return (
    <>
      {anyYearly && (
        <div className="mb-8 flex justify-end">
          <BillingToggle yearly={yearly} onYearly={setYearly} />
        </div>
      )}

      <div className="space-y-12">
        {products.map((product) => (
          <section key={product.serviceCode}>
            <div className="mb-4 flex flex-wrap items-center gap-2">
              <Link
                href={`/product/${product.serviceCode}`}
                className="group flex items-center gap-1 text-lg font-extrabold tracking-tight hover:text-primary"
              >
                {product.name}
                <ChevronRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5 group-hover:text-primary" />
              </Link>
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

            <PlanGrid product={product} yearly={yearly} />
          </section>
        ))}
      </div>
    </>
  );
}
