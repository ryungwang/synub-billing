"use client";

import * as React from "react";
import { Loader2, FileText, Check, X } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Table, Td } from "@/components/admin-table";
import { api, type ApiAdminOrg } from "@/lib/api";

export default function AdminOrganizationsPage() {
  const [orgs, setOrgs] = React.useState<ApiAdminOrg[] | null>(null);
  const [busy, setBusy] = React.useState<number | null>(null);

  const load = React.useCallback(() => {
    api.adminOrganizations().then(setOrgs).catch(() => setOrgs([]));
  }, []);
  React.useEffect(() => load(), [load]);

  async function viewDoc(id: number) {
    try {
      const url = await api.adminOrgDocumentUrl(id);
      window.open(url, "_blank");
    } catch {
      /* noop */
    }
  }
  async function approve(id: number) {
    setBusy(id);
    try {
      await api.adminApproveOrg(id);
      load();
    } finally {
      setBusy(null);
    }
  }
  async function reject(id: number) {
    const reason = window.prompt("반려 사유를 입력하세요.", "사업자등록증 확인 불가");
    if (reason === null) return;
    setBusy(id);
    try {
      await api.adminRejectOrg(id, reason);
      load();
    } finally {
      setBusy(null);
    }
  }

  return (
    <>
      <PageHeader title="관리자 · 회사 심사" description="사업자 등록 서류를 확인하고 인증을 승인/반려합니다." />
      <Card className="p-0">
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
                      onClick={() => approve(o.id)}
                      disabled={busy === o.id}
                    >
                      {busy === o.id ? <Loader2 className="animate-spin" /> : <Check />}
                      승인
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-muted-foreground hover:text-destructive"
                      onClick={() => reject(o.id)}
                      disabled={busy === o.id}
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
    </>
  );
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
