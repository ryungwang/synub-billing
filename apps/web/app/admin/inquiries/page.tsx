"use client";

import * as React from "react";
import { Paperclip, Download, Check, Loader2, Mail } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Table, Td } from "@/components/admin-table";
import { api, type ApiAdminInquiry } from "@/lib/api";
import { cn } from "@/lib/utils";

/** ISO_LOCAL_DATE_TIME("2026-07-06T17:17:06.83") → "2026-07-06 17:17" */
function fmt(iso: string | null): string {
  if (!iso) return "—";
  const [d, t] = iso.split("T");
  return `${d} ${(t ?? "").slice(0, 5)}`;
}

function humanSize(bytes: number | null): string {
  if (!bytes) return "";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export default function AdminInquiriesPage() {
  const [rows, setRows] = React.useState<ApiAdminInquiry[] | null>(null);
  const [busy, setBusy] = React.useState<number | null>(null);
  const [detailId, setDetailId] = React.useState<number | null>(null);
  const detail = rows?.find((r) => r.id === detailId) ?? null;

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
        description="공개 문의 폼(/contact)으로 접수된 문의입니다. 행을 클릭하면 상세를 볼 수 있어요."
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
            <tr
              key={r.id}
              onClick={() => setDetailId(r.id)}
              className="cursor-pointer border-b border-border align-top last:border-0 hover:bg-muted/40"
            >
              <Td className="tnum whitespace-nowrap text-muted-foreground">{fmt(r.createdAt)}</Td>
              <Td className="whitespace-nowrap font-medium">{r.type}</Td>
              <Td className="text-muted-foreground">{r.productLabel ?? "—"}</Td>
              <Td>
                <div className="font-medium">{r.name ?? "—"}</div>
                <span className="text-xs text-muted-foreground">{r.email}</span>
              </Td>
              <Td className="max-w-[22rem]">
                <p className="line-clamp-2 whitespace-pre-wrap text-muted-foreground">{r.message}</p>
              </Td>
              <Td>
                {r.hasAttachment ? (
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      download(r);
                    }}
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
                    onClick={(e) => {
                      e.stopPropagation();
                      resolve(r.id);
                    }}
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

      {/* 문의 상세 모달 */}
      <Dialog open={detail != null} onOpenChange={(v) => !v && setDetailId(null)}>
        <DialogContent className="max-w-lg">
          {detail && (
            <>
              <DialogHeader>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="inline-block rounded-full bg-primary-subtle px-2.5 py-1 text-xs font-bold text-primary">
                    {detail.type}
                  </span>
                  {detail.status === "resolved" ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-success-subtle px-2.5 py-1 text-xs font-semibold text-success-foreground">
                      <Check className="size-3" /> 처리완료
                    </span>
                  ) : (
                    <span className="inline-block rounded-full border border-border px-2.5 py-1 text-xs font-semibold text-muted-foreground">
                      미처리
                    </span>
                  )}
                </div>
                <DialogTitle className="mt-1">{detail.name ?? detail.email}</DialogTitle>
                <DialogDescription>
                  {fmt(detail.createdAt)} · 문의 #{detail.id}
                </DialogDescription>
              </DialogHeader>

              <dl className="grid grid-cols-[84px_1fr] gap-x-3 gap-y-2.5 text-sm">
                <dt className="text-muted-foreground">제품</dt>
                <dd className="font-medium">{detail.productLabel ?? "지정 안 함"}</dd>
                <dt className="text-muted-foreground">회신 이메일</dt>
                <dd>
                  <a href={`mailto:${detail.email}`} className="font-medium text-primary hover:underline">
                    {detail.email}
                  </a>
                </dd>
                {detail.hasAttachment && (
                  <>
                    <dt className="text-muted-foreground">첨부</dt>
                    <dd>
                      <button
                        type="button"
                        onClick={() => download(detail)}
                        disabled={busy === detail.id}
                        className="inline-flex items-center gap-1.5 rounded-lg border border-border px-2.5 py-1.5 text-xs font-semibold transition-colors hover:bg-muted disabled:opacity-50"
                      >
                        {busy === detail.id ? <Loader2 className="size-3.5 animate-spin" /> : <Download className="size-3.5" />}
                        <span className="max-w-[12rem] truncate">{detail.attachmentFilename}</span>
                        {detail.attachmentSize ? (
                          <span className="text-muted-foreground">· {humanSize(detail.attachmentSize)}</span>
                        ) : null}
                      </button>
                    </dd>
                  </>
                )}
                {detail.clientIp && (
                  <>
                    <dt className="text-muted-foreground">접수 IP</dt>
                    <dd className="tnum text-xs text-muted-foreground">{detail.clientIp}</dd>
                  </>
                )}
              </dl>

              <div className="mt-1 rounded-xl border border-border bg-muted/40 p-4">
                <p className="max-h-64 overflow-y-auto whitespace-pre-wrap text-sm leading-relaxed">
                  {detail.message}
                </p>
              </div>

              <div className="mt-4 flex flex-wrap justify-end gap-2">
                <Button asChild variant="outline">
                  <a href={`mailto:${detail.email}?subject=${encodeURIComponent(`Re: [문의:${detail.type}]`)}`}>
                    <Mail className="size-4" /> 답장
                  </a>
                </Button>
                {detail.status !== "resolved" && (
                  <Button onClick={() => resolve(detail.id)} disabled={busy === detail.id}>
                    {busy === detail.id ? <Loader2 className="size-4 animate-spin" /> : <Check className="size-4" />}
                    처리완료로 표시
                  </Button>
                )}
              </div>
            </>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
