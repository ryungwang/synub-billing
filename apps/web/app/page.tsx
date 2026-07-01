"use client";

import * as React from "react";
import Link from "next/link";
import {
  Wallet,
  CalendarClock,
  Repeat,
  PiggyBank,
  ArrowRight,
  ArrowUpRight,
  AlertTriangle,
  Loader2,
} from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ProductIcon } from "@/components/product-icon";
import { UsageBar } from "@/components/usage-bar";
import { MiniBarChart } from "@/components/mini-bar-chart";
import { EmptyState } from "@/components/empty-state";
import { Sparkles } from "lucide-react";
import { contextOrgId, rawContext } from "@/lib/context";
import {
  SubscriptionStatusBadge,
  PaymentStatusBadge,
} from "@/components/status-badge";
import { api, type ApiDashboard } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { formatKRW, formatDate, formatDateTime } from "@/lib/utils";

export default function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = React.useState<ApiDashboard | null>(null);

  React.useEffect(() => {
    // 클라이언트에서 페치해야 로그인 토큰(localStorage)이 실려 본인 데이터가 조회된다.
    api.dashboard().then(setData).catch(() => setData(null));
  }, []);

  const greeting = user?.name
    ? `안녕하세요, ${user.name}님 👋`
    : "안녕하세요 👋";

  if (!data) {
    return (
      <>
        <PageHeader title={greeting} description="구독과 결제 현황을 한눈에 확인하세요." />
        <div className="flex items-center justify-center gap-2 py-24 text-muted-foreground">
          <Loader2 className="size-5 animate-spin" />
          불러오는 중…
        </div>
      </>
    );
  }

  const { summary, activeSubscriptions, recentPayments, spendHistory } = data;
  const pastDue = activeSubscriptions.filter((s) => s.status === "past_due");
  const isOrg = contextOrgId(rawContext()) !== null;
  const empty = activeSubscriptions.length === 0 && recentPayments.length === 0;

  const kpis = [
    {
      label: "이용 중 구독",
      value: `${summary.activeCount}개`,
      icon: Repeat,
      hint: "활성 상태",
    },
    {
      label: "이번 달 결제 예정",
      value: formatKRW(summary.monthlyTotal),
      icon: Wallet,
      hint: "월간 구독 합계",
    },
    {
      label: "다음 결제일",
      value: summary.nextBillingDate
        ? formatDate(summary.nextBillingDate)
        : "—",
      icon: CalendarClock,
      hint: summary.nextBillingProduct ?? "",
    },
    {
      label: "연간 결제 절약액",
      value: formatKRW(summary.savedByYearly),
      icon: PiggyBank,
      hint: "올해 누적 " + formatKRW(summary.paidThisYear),
    },
  ];

  return (
    <>
      <PageHeader
        title={greeting}
        description="구독과 결제 현황을 한눈에 확인하세요."
        action={
          <Button asChild>
            <Link href="/products">
              제품 둘러보기
              <ArrowRight />
            </Link>
          </Button>
        }
      />

      {pastDue.length > 0 && (
        <div className="mb-6 flex items-start gap-3 rounded-2xl border border-warning/40 bg-warning-subtle px-5 py-4">
          <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full bg-warning/15 text-warning">
            <AlertTriangle className="size-[18px]" />
          </span>
          <div className="flex-1">
            <div className="text-sm font-bold text-warning-foreground">
              결제가 지연된 구독이 {pastDue.length}건 있어요
            </div>
            <p className="mt-0.5 text-sm text-warning-foreground/80">
              {pastDue.map((s) => s.product).join(", ")} — 결제수단을
              확인하시면 즉시 정상화됩니다.
            </p>
          </div>
          <Button asChild size="sm" variant="outline">
            <Link href="/payment-methods">카드 확인</Link>
          </Button>
        </div>
      )}

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {kpis.map((k) => {
          const Icon = k.icon;
          return (
            <Card key={k.label} className="p-5">
              <div className="flex items-center justify-between">
                <span className="text-[13px] font-semibold text-muted-foreground">
                  {k.label}
                </span>
                <span className="flex size-9 items-center justify-center rounded-xl bg-primary-subtle text-primary-subtle-foreground">
                  <Icon className="size-[18px]" />
                </span>
              </div>
              <div className="mt-3 text-2xl font-extrabold tracking-tight tnum">
                {k.value}
              </div>
              <div className="mt-1 truncate text-xs text-muted-foreground">
                {k.hint}
              </div>
            </Card>
          );
        })}
      </div>

      {empty ? (
        <div className="mt-6">
          <EmptyState
            icon={Sparkles}
            title={isOrg ? "이 회사에 아직 구독이 없어요" : "아직 구독이 없어요"}
            description={
              isOrg
                ? "회사용 제품을 구독하면 여기에서 함께 관리할 수 있어요."
                : "제품을 둘러보고 첫 구독을 시작해보세요."
            }
            action={
              <Button asChild>
                <Link href="/products">
                  제품 둘러보기
                  <ArrowRight />
                </Link>
              </Button>
            }
          />
        </div>
      ) : (
        <>
      <div className="mt-6 grid gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <div className="flex items-start justify-between p-6 pb-4">
            <div>
              <h2 className="text-base font-bold">지출 추이</h2>
              <p className="mt-0.5 text-[13px] text-muted-foreground">
                최근 6개월 결제 합계
              </p>
            </div>
            <div className="text-right">
              <div className="text-xl font-extrabold tnum">
                {formatKRW(summary.paidThisYear)}
              </div>
              <div className="mt-0.5 flex items-center justify-end gap-1 text-xs font-semibold text-success-foreground">
                <ArrowUpRight className="size-3.5" />
                올해 누적
              </div>
            </div>
          </div>
          <div className="px-6 pb-6">
            <MiniBarChart data={spendHistory} />
          </div>
        </Card>

        <Card className="lg:col-span-2">
          <div className="flex items-center justify-between p-6 pb-3">
            <h2 className="text-base font-bold">이번 달 사용 현황</h2>
          </div>
          <div className="space-y-4 px-6 pb-6">
            {activeSubscriptions
              .filter((s) => s.usage)
              .map((s) => (
                <div key={s.id} className="flex items-center gap-3">
                  <ProductIcon name={s.product} size="sm" />
                  <div className="min-w-0 flex-1">
                    <div className="mb-1 truncate text-[13px] font-bold">
                      {s.product}
                    </div>
                    <UsageBar usage={s.usage!} />
                  </div>
                </div>
              ))}
          </div>
        </Card>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <div className="flex items-center justify-between p-6 pb-2">
            <h2 className="text-base font-bold">내 구독</h2>
            <Button
              asChild
              variant="ghost"
              size="sm"
              className="text-muted-foreground"
            >
              <Link href="/subscriptions">
                전체보기
                <ArrowRight />
              </Link>
            </Button>
          </div>
          <div className="divide-y divide-border px-2 pb-2">
            {activeSubscriptions.map((s) => (
              <div
                key={s.id}
                className="flex items-center gap-4 rounded-xl px-4 py-3.5 transition-colors hover:bg-muted/50"
              >
                <ProductIcon name={s.product} size="md" />
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="truncate font-bold">{s.product}</span>
                    <span className="rounded-md bg-secondary px-1.5 py-0.5 text-[11px] font-bold text-secondary-foreground">
                      {s.plan}
                    </span>
                  </div>
                  <div className="mt-0.5 text-[13px] text-muted-foreground">
                    다음 결제 {formatDate(s.nextBillingDate)}
                  </div>
                </div>
                <div className="text-right">
                  <div className="font-bold tnum">{formatKRW(s.amount)}</div>
                  <div className="mt-1">
                    <SubscriptionStatusBadge status={s.status} />
                  </div>
                </div>
              </div>
            ))}
          </div>
          <div className="mt-1 flex items-center justify-between border-t border-border px-6 py-4">
            <span className="text-sm text-muted-foreground">월 결제 합계</span>
            <span className="text-base font-extrabold tnum">
              {formatKRW(summary.monthlyTotal)}
              <span className="text-xs font-normal text-muted-foreground">
                {" "}
                /월
              </span>
            </span>
          </div>
        </Card>

        <Card className="lg:col-span-2">
          <div className="flex items-center justify-between p-6 pb-2">
            <h2 className="text-base font-bold">최근 결제</h2>
            <Button
              asChild
              variant="ghost"
              size="sm"
              className="text-muted-foreground"
            >
              <Link href="/payments">
                전체보기
                <ArrowRight />
              </Link>
            </Button>
          </div>
          <div className="px-2 pb-2">
            {recentPayments.map((p) => (
              <div
                key={p.id}
                className="flex items-center gap-3 rounded-xl px-4 py-3 transition-colors hover:bg-muted/50"
              >
                <ProductIcon name={p.product} size="sm" />
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
        </Card>
      </div>
        </>
      )}
    </>
  );
}
