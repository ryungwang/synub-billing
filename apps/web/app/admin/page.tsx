"use client";

import * as React from "react";
import { Repeat, Users, Building2, TrendingUp, Wallet } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { api, type ApiAdminStats } from "@/lib/api";
import { formatKRW } from "@/lib/utils";

export default function AdminDashboardPage() {
  const [stats, setStats] = React.useState<ApiAdminStats | null>(null);

  React.useEffect(() => {
    api.adminStats().then(setStats).catch(() => setStats(null));
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
      <PageHeader title="관리자 · 대시보드" description="플랫폼 운영 지표 한눈에 보기." />
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
    </>
  );
}
