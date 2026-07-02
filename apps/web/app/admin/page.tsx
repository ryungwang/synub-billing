"use client";

import * as React from "react";
import { Repeat, Users, Building2, TrendingUp, Wallet } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { AreaTrend, BarTrend, Donut, HBars, type Slice } from "@/components/charts";
import { api, type ApiAdminStats, type ApiAdminAnalytics, type ApiNameValue } from "@/lib/api";
import { formatKRW } from "@/lib/utils";

const SUB_COLORS = ["var(--success)", "var(--warning)", "var(--destructive)", "var(--muted-foreground)"];
const PAY_COLORS = ["var(--success)", "var(--destructive)", "var(--muted-foreground)"];
const ORG_COLORS = ["var(--success)", "var(--warning)", "var(--destructive)"];
const toSlices = (arr: ApiNameValue[], colors: string[]): Slice[] =>
  arr.map((nv, i) => ({ name: nv.name, value: nv.value, color: colors[i] ?? "var(--muted-foreground)" }));

function Panel({ title, sub, children }: { title: string; sub?: string; children: React.ReactNode }) {
  return (
    <Card className="p-5">
      <div className="mb-4">
        <h2 className="text-sm font-bold">{title}</h2>
        {sub && <p className="mt-0.5 text-xs text-muted-foreground">{sub}</p>}
      </div>
      {children}
    </Card>
  );
}

export default function AdminDashboardPage() {
  const [stats, setStats] = React.useState<ApiAdminStats | null>(null);
  const [an, setAn] = React.useState<ApiAdminAnalytics | null>(null);

  React.useEffect(() => {
    api.adminStats().then(setStats).catch(() => setStats(null));
    api.adminAnalytics().then(setAn).catch(() => setAn(null));
  }, []);

  const kpis = stats
    ? [
        { label: "활성 구독", value: `${stats.activeSubscriptions}건`, icon: Repeat },
        { label: "월 반복 매출(MRR)", value: formatKRW(stats.monthlyRevenue), icon: TrendingUp },
        { label: "이번 달 결제", value: formatKRW(stats.paidThisMonth), icon: Wallet },
        { label: "고객", value: `${stats.customers}명`, icon: Users },
        { label: "조직", value: `${stats.organizations}개`, icon: Building2 },
      ]
    : [];

  return (
    <>
      <PageHeader title="관리자 · 대시보드" description="플랫폼 운영 지표와 추세를 한눈에." />

      {/* KPI */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
        {kpis.map((k) => {
          const Icon = k.icon;
          return (
            <Card key={k.label} className="p-5">
              <div className="flex items-center justify-between">
                <span className="text-[13px] font-semibold text-muted-foreground">{k.label}</span>
                <span className="flex size-9 items-center justify-center rounded-xl bg-primary-subtle text-primary-subtle-foreground">
                  <Icon className="size-[18px]" />
                </span>
              </div>
              <div className="mt-3 text-2xl font-extrabold tracking-tight tnum">{k.value}</div>
            </Card>
          );
        })}
      </div>

      {/* 매출 추세 */}
      <div className="mt-4">
        <Panel title="월별 결제 매출" sub="최근 6개월 · 결제 완료 기준">
          {an ? <AreaTrend data={an.revenueTrend.map((p) => ({ label: p.month, value: p.amount }))} />
              : <ChartSkeleton h={200} />}
        </Panel>
      </div>

      {/* 신규 구독 · 제품별 매출 */}
      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <Panel title="신규 구독" sub="최근 6개월">
          {an ? <BarTrend unit="건" data={an.subsTrend.map((p) => ({ label: p.month, value: p.count }))} />
              : <ChartSkeleton h={180} />}
        </Panel>
        <Panel title="제품별 매출" sub="결제 완료 누적">
          {an ? <HBars money data={an.revenueByProduct.map((nv) => ({ label: nv.name, value: nv.value }))} />
              : <ChartSkeleton h={180} />}
        </Panel>
      </div>

      {/* 상태 분포 3종 */}
      <div className="mt-4 grid gap-4 md:grid-cols-3">
        <Panel title="구독 상태">
          {an ? <Donut unit="건" slices={toSlices(an.subsByStatus, SUB_COLORS)} /> : <ChartSkeleton h={148} />}
        </Panel>
        <Panel title="결제 상태">
          {an ? <Donut unit="건" slices={toSlices(an.paymentsByStatus, PAY_COLORS)} /> : <ChartSkeleton h={148} />}
        </Panel>
        <Panel title="회사 인증">
          {an ? <Donut unit="곳" slices={toSlices(an.orgsByStatus, ORG_COLORS)} /> : <ChartSkeleton h={148} />}
        </Panel>
      </div>
    </>
  );
}

function ChartSkeleton({ h }: { h: number }) {
  return <div className="animate-pulse rounded-xl bg-muted/60" style={{ height: h }} />;
}
