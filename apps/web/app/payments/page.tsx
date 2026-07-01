"use client";

import * as React from "react";
import { Download, Receipt, Search, Loader2 } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { PaymentStatusBadge, type PayStatus } from "@/components/status-badge";
import { ProductIcon } from "@/components/product-icon";
import { api, type ApiPayment } from "@/lib/api";
import { cn, formatKRW, formatDateTime } from "@/lib/utils";

const FILTERS: { key: PayStatus | "all"; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "paid", label: "결제완료" },
  { key: "failed", label: "실패" },
  { key: "refunded", label: "환불" },
];

export default function PaymentsPage() {
  const [payments, setPayments] = React.useState<ApiPayment[] | null>(null);
  const [filter, setFilter] = React.useState<PayStatus | "all">("all");

  React.useEffect(() => {
    api.payments().then(setPayments).catch(() => setPayments([]));
  }, []);

  const all = payments ?? [];
  const rows = filter === "all" ? all : all.filter((p) => p.status === filter);
  const totalPaid = all
    .filter((p) => p.status === "paid")
    .reduce((s, p) => s + p.amount, 0);

  return (
    <>
      <PageHeader
        title="결제 내역"
        description="모든 결제 시도와 영수증을 확인할 수 있어요."
        action={
          <Button variant="outline">
            <Download />
            내역 다운로드
          </Button>
        }
      />

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="inline-flex items-center rounded-full border border-border bg-card p-1">
          {FILTERS.map((f) => (
            <button
              key={f.key}
              onClick={() => setFilter(f.key)}
              className={cn(
                "rounded-full px-3.5 py-1.5 text-[13px] font-semibold transition-all",
                filter === f.key
                  ? "bg-secondary text-secondary-foreground"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              {f.label}
            </button>
          ))}
        </div>
        <div className="text-sm text-muted-foreground">
          결제완료 합계{" "}
          <span className="font-extrabold text-foreground tnum">
            {formatKRW(totalPaid)}
          </span>
        </div>
      </div>

      <Card className="overflow-hidden p-0">
        <div className="hidden sm:block">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/40 text-left text-xs font-bold uppercase tracking-wide text-muted-foreground">
                <th className="px-6 py-3.5">제품</th>
                <th className="px-6 py-3.5">결제일시</th>
                <th className="px-6 py-3.5">결제수단</th>
                <th className="px-6 py-3.5 text-right">금액</th>
                <th className="px-6 py-3.5 text-center">상태</th>
                <th className="px-6 py-3.5 text-right">영수증</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {rows.map((p) => (
                <tr key={p.id} className="transition-colors hover:bg-muted/30">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <ProductIcon name={p.product} size="sm" />
                      <div>
                        <div className="font-semibold">{p.product}</div>
                        <div className="text-xs text-muted-foreground">
                          {p.plan} 플랜
                        </div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-muted-foreground tnum">
                    {formatDateTime(p.date)}
                  </td>
                  <td className="px-6 py-4 text-muted-foreground">
                    {p.method}
                  </td>
                  <td className="px-6 py-4 text-right font-bold tnum">
                    {formatKRW(p.amount)}
                  </td>
                  <td className="px-6 py-4 text-center">
                    <div className="flex justify-center">
                      <PaymentStatusBadge status={p.status} />
                    </div>
                  </td>
                  <td className="px-6 py-4 text-right">
                    {p.status === "paid" || p.status === "refunded" ? (
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-muted-foreground"
                      >
                        <Receipt />
                        보기
                      </Button>
                    ) : (
                      <span className="text-xs text-muted-foreground">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="divide-y divide-border sm:hidden">
          {rows.map((p) => (
            <div key={p.id} className="flex items-center gap-3 p-4">
              <ProductIcon name={p.product} size="md" />
              <div className="min-w-0 flex-1">
                <div className="truncate text-sm font-semibold">
                  {p.product}
                </div>
                <div className="text-xs text-muted-foreground tnum">
                  {formatDateTime(p.date)}
                </div>
              </div>
              <div className="text-right">
                <div className="text-sm font-bold tnum">
                  {formatKRW(p.amount)}
                </div>
                <div className="mt-1 flex justify-end">
                  <PaymentStatusBadge status={p.status} />
                </div>
              </div>
            </div>
          ))}
        </div>

        {payments === null && (
          <div className="flex items-center justify-center gap-2 py-16 text-muted-foreground">
            <Loader2 className="size-5 animate-spin" />
            불러오는 중…
          </div>
        )}
        {payments !== null && rows.length === 0 && (
          <div className="flex flex-col items-center gap-2 py-16 text-center">
            <Search className="size-8 text-muted-foreground/50" />
            <p className="text-sm text-muted-foreground">
              해당하는 결제 내역이 없어요.
            </p>
          </div>
        )}
      </Card>
    </>
  );
}
