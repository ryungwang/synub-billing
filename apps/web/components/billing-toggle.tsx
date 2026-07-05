"use client";

import { cn } from "@/lib/utils";

/** 월간/연간 결제주기 토글 — 공개 요금 페이지·제품 상세 공용. 상태는 부모가 소유. */
export function BillingToggle({
  yearly,
  onYearly,
}: {
  yearly: boolean;
  onYearly: (v: boolean) => void;
}) {
  return (
    <div className="inline-flex items-center rounded-full border border-border bg-card p-1">
      <button
        type="button"
        onClick={() => onYearly(false)}
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
        onClick={() => onYearly(true)}
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
  );
}
