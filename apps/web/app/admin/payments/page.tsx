"use client";

import * as React from "react";
import { Loader2, RotateCcw } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { PaymentStatusBadge } from "@/components/status-badge";
import { Table, Td } from "@/components/admin-table";
import { api, type ApiAdminPayment } from "@/lib/api";
import { formatKRW, formatDateTime } from "@/lib/utils";

export default function AdminPaymentsPage() {
  const [pays, setPays] = React.useState<ApiAdminPayment[] | null>(null);
  const [refunding, setRefunding] = React.useState<number | null>(null);

  const load = React.useCallback(() => {
    api.adminPayments().then(setPays).catch(() => setPays([]));
  }, []);
  React.useEffect(() => load(), [load]);

  async function refund(id: number) {
    setRefunding(id);
    try {
      await api.adminRefund(id);
      load();
    } catch {
      /* 실패 시 상태 유지 */
    } finally {
      setRefunding(null);
    }
  }

  return (
    <>
      <PageHeader title="관리자 · 결제" description="최근 결제 내역과 환불 처리." />
      <Card className="p-0">
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
    </>
  );
}
