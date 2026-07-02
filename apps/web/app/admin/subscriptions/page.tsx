"use client";

import * as React from "react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { SubscriptionStatusBadge } from "@/components/status-badge";
import { Table, Td } from "@/components/admin-table";
import { api, type ApiAdminSubscription } from "@/lib/api";
import { formatKRW, formatDate } from "@/lib/utils";

export default function AdminSubscriptionsPage() {
  const [subs, setSubs] = React.useState<ApiAdminSubscription[] | null>(null);
  React.useEffect(() => {
    api.adminSubscriptions().then(setSubs).catch(() => setSubs([]));
  }, []);

  return (
    <>
      <PageHeader title="관리자 · 구독" description="전체 구독 현황(개인·회사)." />
      <Card className="p-0">
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
    </>
  );
}
