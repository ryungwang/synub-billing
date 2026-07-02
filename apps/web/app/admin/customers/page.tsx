"use client";

import * as React from "react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { Table, Td } from "@/components/admin-table";
import { api, type ApiAdminCustomer } from "@/lib/api";

export default function AdminCustomersPage() {
  const [customers, setCustomers] = React.useState<ApiAdminCustomer[] | null>(null);

  React.useEffect(() => {
    api.adminCustomers().then(setCustomers).catch(() => setCustomers([]));
  }, []);

  return (
    <>
      <PageHeader
        title="관리자 · 개인 고객"
        description="개인(SSO 통합계정) 결제 고객 목록입니다. 회사(조직)는 '회사 심사'에서 확인하세요."
      />
      <Card className="p-0">
        <div className="border-b border-border px-5 py-3.5 text-sm font-bold">
          개인 고객 {customers ? <span className="text-muted-foreground">{customers.length}</span> : null}
        </div>
        <Table
          rows={customers}
          cols={["이메일", "통합계정 ID", "전화", "구독", "가입일"]}
          render={(c: ApiAdminCustomer) => (
            <tr key={c.id} className="border-b border-border last:border-0 hover:bg-muted/40">
              <Td className="font-medium">{c.email ?? "—"}</Td>
              <Td className="tnum text-muted-foreground">{c.externalId}</Td>
              <Td className="tnum text-muted-foreground">{c.phone ?? "—"}</Td>
              <Td className="tnum">{c.subscriptions > 0 ? `${c.subscriptions}건` : "—"}</Td>
              <Td className="tnum text-muted-foreground">{c.createdAt ?? "—"}</Td>
            </tr>
          )}
        />
      </Card>
    </>
  );
}
