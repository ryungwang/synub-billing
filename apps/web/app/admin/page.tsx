"use client";

import * as React from "react";
import {
  Repeat,
  Users,
  Building2,
  TrendingUp,
  Wallet,
  Loader2,
  ShieldAlert,
  RotateCcw,
  FileText,
  Check,
  X,
} from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/empty-state";
import { AdminProducts } from "@/components/admin-products";
import {
  SubscriptionStatusBadge,
  PaymentStatusBadge,
} from "@/components/status-badge";
import {
  api,
  type ApiAdminStats,
  type ApiAdminSubscription,
  type ApiAdminPayment,
  type ApiAdminOrg,
} from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { formatKRW, formatDate, formatDateTime } from "@/lib/utils";

export default function AdminPage() {
  const { user } = useAuth();
  const [stats, setStats] = React.useState<ApiAdminStats | null>(null);
  const [subs, setSubs] = React.useState<ApiAdminSubscription[] | null>(null);
  const [pays, setPays] = React.useState<ApiAdminPayment[] | null>(null);
  const [orgs, setOrgs] = React.useState<ApiAdminOrg[] | null>(null);
  const [refunding, setRefunding] = React.useState<number | null>(null);
  const [orgBusy, setOrgBusy] = React.useState<number | null>(null);

  const load = React.useCallback(() => {
    api.adminStats().then(setStats).catch(() => setStats(null));
    api.adminSubscriptions().then(setSubs).catch(() => setSubs([]));
    api.adminPayments().then(setPays).catch(() => setPays([]));
    api.adminOrganizations().then(setOrgs).catch(() => setOrgs([]));
  }, []);

  async function viewDoc(id: number) {
    try {
      const url = await api.adminOrgDocumentUrl(id);
      window.open(url, "_blank");
    } catch {
      /* noop */
    }
  }
  async function approveOrg(id: number) {
    setOrgBusy(id);
    try {
      await api.adminApproveOrg(id);
      load();
    } finally {
      setOrgBusy(null);
    }
  }
  async function rejectOrg(id: number) {
    const reason = window.prompt("반려 사유를 입력하세요.", "사업자등록증 확인 불가");
    if (reason === null) return;
    setOrgBusy(id);
    try {
      await api.adminRejectOrg(id, reason);
      load();
    } finally {
      setOrgBusy(null);
    }
  }

  React.useEffect(() => {
    if (user?.admin) load();
  }, [user?.admin, load]);

  if (user && !user.admin) {
    return (
      <>
        <PageHeader title="관리자" description="플랫폼 운영 콘솔" />
        <EmptyState
          icon={ShieldAlert}
          title="관리자 전용 페이지입니다"
          description="이 페이지에 접근할 권한이 없습니다."
        />
      </>
    );
  }

  async function refund(id: number) {
    setRefunding(id);
    try {
      await api.adminRefund(id);
      load();
    } catch {
      /* 에러는 무시(간단화) — 실패 시 상태 유지 */
    } finally {
      setRefunding(null);
    }
  }

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
      <PageHeader
        title="관리자 콘솔"
        description="전체 구독·결제 현황을 확인하고 환불을 처리하세요."
      />

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
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
            </Card>
          );
        })}
      </div>

      <Card className="mt-6 p-0">
        <div className="border-b border-border px-5 py-3.5 text-sm font-bold">
          회사 인증 심사 {orgs ? <span className="text-muted-foreground">{orgs.length}</span> : null}
        </div>
        <Table
          rows={orgs}
          cols={["회사", "사업자등록번호", "조직코드", "상태", ""]}
          render={(o: ApiAdminOrg) => (
            <tr key={o.id} className="border-b border-border last:border-0 hover:bg-muted/40">
              <Td className="font-medium">{o.name}</Td>
              <Td className="tnum text-muted-foreground">{o.businessNo ?? "—"}</Td>
              <Td className="tnum font-semibold">{o.orgCode ?? "—"}</Td>
              <Td><OrgStatusBadge status={o.verifyStatus} reason={o.rejectReason} /></Td>
              <Td className="text-right">
                {o.businessNo && (
                  <Button variant="ghost" size="sm" onClick={() => viewDoc(o.id)}>
                    <FileText />
                    서류
                  </Button>
                )}
                {o.verifyStatus === "pending" && (
                  <>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-success-foreground"
                      onClick={() => approveOrg(o.id)}
                      disabled={orgBusy === o.id}
                    >
                      {orgBusy === o.id ? <Loader2 className="animate-spin" /> : <Check />}
                      승인
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-muted-foreground hover:text-destructive"
                      onClick={() => rejectOrg(o.id)}
                      disabled={orgBusy === o.id}
                    >
                      <X />
                      반려
                    </Button>
                  </>
                )}
              </Td>
            </tr>
          )}
        />
      </Card>

      <Card className="mt-6 p-0">
        <div className="border-b border-border px-5 py-3.5 text-sm font-bold">
          전체 구독 {subs ? <span className="text-muted-foreground">{subs.length}</span> : null}
        </div>
        <Table
          rows={subs}
          cols={["고객", "제품 · 플랜", "소유", "상태", "금액", "다음 결제"]}
          render={(s: ApiAdminSubscription) => (
            <tr key={s.id} className="border-b border-border last:border-0 hover:bg-muted/40">
              <Td className="font-medium">{s.customerEmail}</Td>
              <Td>
                {s.product} <span className="text-muted-foreground">· {s.plan}</span>
              </Td>
              <Td>{s.ownerType === "organization" ? "회사" : "개인"}</Td>
              <Td><SubscriptionStatusBadge status={s.status} /></Td>
              <Td className="tnum font-semibold">{formatKRW(s.amount)}</Td>
              <Td className="tnum text-muted-foreground">{formatDate(s.nextBillingDate)}</Td>
            </tr>
          )}
        />
      </Card>

      <Card className="mt-6 p-0">
        <div className="border-b border-border px-5 py-3.5 text-sm font-bold">
          최근 결제 {pays ? <span className="text-muted-foreground">{pays.length}</span> : null}
        </div>
        <Table
          rows={pays}
          cols={["고객", "제품", "금액", "상태", "일시", ""]}
          render={(p: ApiAdminPayment) => (
            <tr key={p.id} className="border-b border-border last:border-0 hover:bg-muted/40">
              <Td className="font-medium">{p.customerEmail}</Td>
              <Td>{p.product}</Td>
              <Td className="tnum font-semibold">{formatKRW(p.amount)}</Td>
              <Td><PaymentStatusBadge status={p.status} /></Td>
              <Td className="tnum text-muted-foreground">{formatDateTime(p.date)}</Td>
              <Td className="text-right">
                {p.status === "paid" && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-muted-foreground hover:text-destructive"
                    onClick={() => refund(p.id)}
                    disabled={refunding === p.id}
                  >
                    {refunding === p.id ? <Loader2 className="animate-spin" /> : <RotateCcw />}
                    환불
                  </Button>
                )}
              </Td>
            </tr>
          )}
        />
      </Card>

      <AdminProducts />
    </>
  );
}

function Table<T>({
  rows,
  cols,
  render,
}: {
  rows: T[] | null;
  cols: string[];
  render: (row: T) => React.ReactNode;
}) {
  if (rows === null) {
    return (
      <div className="flex items-center justify-center gap-2 py-12 text-muted-foreground">
        <Loader2 className="size-5 animate-spin" /> 불러오는 중…
      </div>
    );
  }
  if (rows.length === 0) {
    return <div className="py-10 text-center text-sm text-muted-foreground">데이터가 없습니다.</div>;
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border text-left text-[11px] font-bold uppercase tracking-wide text-muted-foreground">
            {cols.map((c, i) => (
              <th key={i} className="px-5 py-2.5 font-bold">
                {c}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{rows.map(render)}</tbody>
      </table>
    </div>
  );
}

function Td({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return <td className={"px-5 py-3 " + (className ?? "")}>{children}</td>;
}

function OrgStatusBadge({ status, reason }: { status: string; reason: string | null }) {
  const map: Record<string, { label: string; cls: string }> = {
    verified: { label: "인증 완료", cls: "bg-success-subtle text-success-foreground" },
    pending: { label: "심사 대기", cls: "bg-warning-subtle text-warning-foreground" },
    rejected: { label: "반려", cls: "bg-destructive-subtle text-destructive-subtle-foreground" },
  };
  const s = map[status] ?? map.pending;
  return (
    <span
      className={"inline-flex rounded-md px-1.5 py-0.5 text-[11px] font-bold " + s.cls}
      title={reason ?? undefined}
    >
      {s.label}
    </span>
  );
}
