"use client";

import * as React from "react";
import { Paperclip, Download, Check, Loader2 } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { Table, Td } from "@/components/admin-table";
import { api, type ApiAdminInquiry } from "@/lib/api";
import { cn } from "@/lib/utils";

/** ISO_LOCAL_DATE_TIME("2026-07-06T17:17:06.83") → "2026-07-06 17:17" */
function fmt(iso: string | null): string {
  if (!iso) return "—";
  const [d, t] = iso.split("T");
  return `${d} ${(t ?? "").slice(0, 5)}`;
}

export default function AdminInquiriesPage() {
  const [rows, setRows] = React.useState<ApiAdminInquiry[] | null>(null);
  const [busy, setBusy] = React.useState<number | null>(null);

  React.useEffect(() => {
    api.adminInquiries().then(setRows).catch(() => setRows([]));
  }, []);

  async function download(inq: ApiAdminInquiry) {
    setBusy(inq.id);
    try {
      const url = await api.adminInquiryAttachmentUrl(inq.id);
      const a = document.createElement("a");
      a.href = url;
      a.download = inq.attachmentFilename ?? `inquiry-${inq.id}`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 10_000);
    } catch {
      /* noop — 실패 시 조용히 무시(관리자 콘솔) */
    } finally {
      setBusy(null);
    }
  }

  async function resolve(id: number) {
    setBusy(id);
    try {
      await api.adminResolveInquiry(id);
      setRows((prev) =>
        prev ? prev.map((r) => (r.id === id ? { ...r, status: "resolved" } : r)) : prev
      );
    } catch {
      /* noop */
    } finally {
      setBusy(null);
    }
  }

  const open = rows?.filter((r) => r.status !== "resolved").length ?? 0;

  return (
    <>
      <PageHeader
        title="관리자 · 문의"
        description="공개 문의 폼(/contact)으로 접수된 문의입니다. 첨부파일을 내려받고 처리 완료로 표시할 수 있습니다."
      />
      <Card className="p-0">
        <div className="border-b border-border px-5 py-3.5 text-sm font-bold">
          문의{" "}
          {rows ? (
            <span className="text-muted-foreground">
              {rows.length} · 미처리 {open}
            </span>
          ) : null}
        </div>
        <Table
          rows={rows}
          cols={["접수일", "유형", "제품", "문의자", "내용", "첨부", "상태"]}
          render={(r: ApiAdminInquiry) => (
            <tr key={r.id} className="border-b border-border align-top last:border-0 hover:bg-muted/40">
              <Td className="tnum whitespace-nowrap text-muted-foreground">{fmt(r.createdAt)}</Td>
              <Td className="whitespace-nowrap font-medium">{r.type}</Td>
              <Td className="text-muted-foreground">{r.productLabel ?? "—"}</Td>
              <Td>
                <div className="font-medium">{r.name ?? "—"}</div>
                <a href={`mailto:${r.email}`} className="text-xs text-muted-foreground hover:underline">
                  {r.email}
                </a>
              </Td>
              <Td className="max-w-[22rem]">
                <p className="line-clamp-3 whitespace-pre-wrap text-muted-foreground">{r.message}</p>
              </Td>
              <Td>
                {r.hasAttachment ? (
                  <button
                    type="button"
                    onClick={() => download(r)}
                    disabled={busy === r.id}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-border px-2.5 py-1.5 text-xs font-semibold transition-colors hover:bg-muted disabled:opacity-50"
                    title={r.attachmentFilename ?? undefined}
                  >
                    {busy === r.id ? <Loader2 className="size-3.5 animate-spin" /> : <Download className="size-3.5" />}
                    <span className="max-w-[8rem] truncate">{r.attachmentFilename ?? "다운로드"}</span>
                  </button>
                ) : (
                  <span className="inline-flex items-center gap-1 text-xs text-muted-foreground/60">
                    <Paperclip className="size-3.5" />—
                  </span>
                )}
              </Td>
              <Td className="whitespace-nowrap">
                {r.status === "resolved" ? (
                  <span className="inline-flex items-center gap-1 rounded-full bg-success-subtle px-2.5 py-1 text-xs font-semibold text-success-foreground">
                    <Check className="size-3" /> 처리완료
                  </span>
                ) : (
                  <button
                    type="button"
                    onClick={() => resolve(r.id)}
                    disabled={busy === r.id}
                    className={cn(
                      "inline-flex items-center gap-1 rounded-full border border-border px-2.5 py-1 text-xs font-semibold text-muted-foreground transition-colors hover:text-foreground disabled:opacity-50"
                    )}
                  >
                    {busy === r.id ? <Loader2 className="size-3 animate-spin" /> : null}
                    처리완료로 표시
                  </button>
                )}
              </Td>
            </tr>
          )}
        />
      </Card>
    </>
  );
}
