"use client";

import * as React from "react";
import { useSyncExternalStore } from "react";
import {
  Check,
  ChevronsUpDown,
  User,
  Building2,
  Plus,
  Loader2,
  AlertCircle,
  Mail,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { api, type ApiOrg, type ApiInvitation } from "@/lib/api";
import { verifyRepresentative, identityConfigured } from "@/lib/portone";
import {
  rawContext,
  setContext,
  subscribeContext,
  contextOrgId,
} from "@/lib/context";
import { cn } from "@/lib/utils";

const ROLE_LABEL: Record<string, string> = {
  owner: "소유자",
  billing_manager: "결제 관리자",
  member: "멤버",
};

export function ContextSwitcher() {
  const ctx = useSyncExternalStore(subscribeContext, rawContext, () => "personal");
  const [orgs, setOrgs] = React.useState<ApiOrg[]>([]);
  const [invites, setInvites] = React.useState<ApiInvitation[]>([]);
  const [open, setOpen] = React.useState(false);
  const [createOpen, setCreateOpen] = React.useState(false);

  React.useEffect(() => {
    api.organizations().then(setOrgs).catch(() => setOrgs([]));
    api.myInvitations().then(setInvites).catch(() => setInvites([]));
  }, []);

  function acceptInvite(id: number) {
    api.acceptInvitation(id).then(() => window.location.reload());
  }
  function declineInvite(id: number) {
    api.declineInvitation(id).then(() => setInvites((v) => v.filter((i) => i.id !== id)));
  }

  const orgId = contextOrgId(ctx);
  const currentOrg = orgId ? orgs.find((o) => o.id === orgId) : null;
  const isPersonal = orgId === null;

  function choose(value: string) {
    setOpen(false);
    if (value === ctx) return;
    setContext(value);
    window.location.reload(); // 새 컨텍스트로 전체 데이터 갱신
  }

  return (
    <div className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className="flex items-center gap-2 rounded-full border border-border bg-card py-1.5 pl-2.5 pr-2 text-sm font-semibold transition-colors hover:bg-muted"
      >
        <span className="flex size-6 items-center justify-center rounded-md bg-primary-subtle text-primary-subtle-foreground">
          {isPersonal ? <User className="size-3.5" /> : <Building2 className="size-3.5" />}
        </span>
        <span className="hidden max-w-[120px] truncate sm:block">
          {isPersonal ? "개인" : currentOrg?.name ?? "회사"}
        </span>
        <ChevronsUpDown className="size-3.5 text-muted-foreground" />
        {invites.length > 0 && (
          <span className="ml-0.5 flex size-4 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground">
            {invites.length}
          </span>
        )}
      </button>

      {open && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} />
          <div className="absolute left-0 top-[calc(100%+8px)] z-50 w-60 overflow-hidden rounded-2xl border border-border bg-card p-1.5 shadow-lg animate-in fade-in-0 slide-in-from-top-1">
            <div className="px-2.5 py-1.5 text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
              컨텍스트 전환
            </div>

            <Row
              icon={<User className="size-4" />}
              label="개인"
              hint="내 카드·내 구독"
              active={isPersonal}
              onClick={() => choose("personal")}
            />

            {orgs.map((o) => (
              <Row
                key={o.id}
                icon={<Building2 className="size-4" />}
                label={o.name}
                hint={
                  o.verifyStatus === "verified"
                    ? o.orgCode
                      ? `${ROLE_LABEL[o.role] ?? o.role} · ${o.orgCode}`
                      : ROLE_LABEL[o.role] ?? o.role
                    : o.verifyStatus === "pending"
                    ? "인증 심사중"
                    : "인증 반려"
                }
                active={orgId === o.id}
                onClick={() => choose(`org:${o.id}`)}
              />
            ))}

            {invites.length > 0 && (
              <>
                <div className="my-1 h-px bg-border" />
                <div className="px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider text-muted-foreground">
                  받은 초대
                </div>
                {invites.map((inv) => (
                  <div key={inv.id} className="flex items-center gap-2 rounded-xl px-2.5 py-2">
                    <span className="flex size-7 items-center justify-center rounded-lg bg-muted text-muted-foreground">
                      <Mail className="size-4" />
                    </span>
                    <div className="min-w-0 flex-1">
                      <div className="truncate text-sm font-bold">
                        {inv.organizationName ?? "회사"}
                      </div>
                      <div className="truncate text-[11px] text-muted-foreground">
                        {ROLE_LABEL[inv.role] ?? inv.role} 초대
                      </div>
                    </div>
                    <button
                      onClick={() => acceptInvite(inv.id)}
                      className="flex size-6 items-center justify-center rounded-md bg-primary text-primary-foreground hover:opacity-90"
                      aria-label="수락"
                    >
                      <Check className="size-3.5" />
                    </button>
                    <button
                      onClick={() => declineInvite(inv.id)}
                      className="flex size-6 items-center justify-center rounded-md text-muted-foreground hover:bg-muted"
                      aria-label="거절"
                    >
                      <X className="size-3.5" />
                    </button>
                  </div>
                ))}
              </>
            )}

            <div className="my-1 h-px bg-border" />
            <button
              onClick={() => {
                setOpen(false);
                setCreateOpen(true);
              }}
              className="flex w-full items-center gap-2.5 rounded-xl px-2.5 py-2 text-sm font-medium text-primary transition-colors hover:bg-primary-subtle/40"
            >
              <span className="flex size-7 items-center justify-center rounded-lg bg-muted">
                <Plus className="size-4" />
              </span>
              회사 만들기
            </button>
          </div>
        </>
      )}

      <CreateOrgDialog open={createOpen} onOpenChange={setCreateOpen} />
    </div>
  );
}

function Row({
  icon,
  label,
  hint,
  active,
  onClick,
}: {
  icon: React.ReactNode;
  label: string;
  hint: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "flex w-full items-center gap-2.5 rounded-xl px-2.5 py-2 text-left transition-colors hover:bg-muted",
        active && "bg-muted"
      )}
    >
      <span className="flex size-7 items-center justify-center rounded-lg bg-primary-subtle text-primary-subtle-foreground">
        {icon}
      </span>
      <span className="min-w-0 flex-1">
        <span className="block truncate text-sm font-bold">{label}</span>
        <span className="block truncate text-[11px] text-muted-foreground">{hint}</span>
      </span>
      {active && <Check className="size-4 text-primary" />}
    </button>
  );
}

const INPUT_CLS =
  "w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm outline-none transition-colors placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20";

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-[13px] font-semibold text-foreground">{label}</span>
      {children}
    </label>
  );
}

function CreateOrgDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const [name, setName] = React.useState("");
  const [corpType, setCorpType] = React.useState<"corp" | "individual">("corp");
  const [corpNo, setCorpNo] = React.useState("");
  const [businessNo, setBusinessNo] = React.useState("");
  const [repName, setRepName] = React.useState("");
  const [openDate, setOpenDate] = React.useState("");
  const [file, setFile] = React.useState<File | null>(null);
  const [idvId, setIdvId] = React.useState<string | null>(null);
  const [verifying, setVerifying] = React.useState(false);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  // 본인인증 필수 — idvId 없으면 제출 불가. 법인이면 법인등록번호 13자리 필수.
  const ready =
    name.trim() && businessNo.trim() && repName.trim() && openDate.trim() && file && idvId &&
    (corpType === "individual" || corpNo.replace(/\D/g, "").length === 13);

  async function verifyRep() {
    setVerifying(true);
    setError(null);
    try {
      const id = await verifyRepresentative();
      if (id) setIdvId(id);
    } catch (err) {
      setError(err instanceof Error ? err.message : "본인인증에 실패했습니다.");
    } finally {
      setVerifying(false);
    }
  }

  async function create(e: React.FormEvent) {
    e.preventDefault();
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      await api.createOrganization({
        name: name.trim(),
        businessNo: businessNo.trim(),
        repName: repName.trim(),
        openDate: openDate.trim(),
        corpType,
        corpNo: corpType === "corp" ? corpNo.replace(/\D/g, "") : undefined,
        document: file,
        identityVerificationId: idvId ?? undefined,
      });
      // 본인인증 통과면 즉시 인증완료, 아니면 심사중(pending) — 목록 갱신
      window.location.reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "회사 생성에 실패했습니다.");
      setBusy(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <div className="mb-1 flex size-12 items-center justify-center rounded-2xl bg-primary-subtle text-primary">
            <Building2 className="size-6" />
          </div>
          <DialogTitle>회사 만들기</DialogTitle>
          <DialogDescription>
            국세청 진위확인 + 대표자 본인인증으로 회사를 인증합니다. 본인인증 시 즉시,
            서류(사업자등록증)만 제출하면 관리자 심사 후 인증됩니다.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={create} className="space-y-3">
          <Field label="회사 이름">
            <input
              autoFocus
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="예: 우리회사"
              required
              className={INPUT_CLS}
            />
          </Field>
          <Field label="사업자 구분">
            <div className="grid grid-cols-2 gap-2">
              {([["corp", "법인"], ["individual", "개인사업자"]] as const).map(
                ([v, lb]) => (
                  <button
                    key={v}
                    type="button"
                    onClick={() => setCorpType(v)}
                    className={cn(
                      "rounded-xl border px-3 py-2.5 text-sm font-semibold transition-colors",
                      corpType === v
                        ? "border-primary bg-primary-subtle text-primary"
                        : "border-border text-muted-foreground hover:bg-muted"
                    )}
                  >
                    {lb}
                  </button>
                )
              )}
            </div>
          </Field>
          <Field label="사업자등록번호">
            <input
              value={businessNo}
              onChange={(e) => setBusinessNo(e.target.value)}
              placeholder="000-00-00000"
              inputMode="numeric"
              required
              className={INPUT_CLS}
            />
          </Field>
          {corpType === "corp" && (
            <Field label="법인등록번호">
              <input
                value={corpNo}
                onChange={(e) => setCorpNo(e.target.value)}
                placeholder="000000-0000000 (13자리)"
                inputMode="numeric"
                required
                className={INPUT_CLS}
              />
            </Field>
          )}
          <div className="grid grid-cols-2 gap-2">
            <Field label="대표자명">
              <input
                value={repName}
                onChange={(e) => setRepName(e.target.value)}
                placeholder="홍길동"
                required
                className={INPUT_CLS}
              />
            </Field>
            <Field label="개업일자">
              <input
                value={openDate}
                onChange={(e) => setOpenDate(e.target.value)}
                placeholder="YYYYMMDD"
                inputMode="numeric"
                required
                className={INPUT_CLS}
              />
            </Field>
          </div>

          {identityConfigured && (
            <button
              type="button"
              onClick={verifyRep}
              disabled={verifying || !!idvId}
              className={cn(
                "flex w-full items-center justify-center gap-2 rounded-xl border px-3.5 py-2.5 text-sm font-semibold transition-colors",
                idvId
                  ? "border-success/40 bg-success-subtle text-success-foreground"
                  : "border-border hover:bg-muted"
              )}
            >
              {verifying ? (
                <Loader2 className="size-4 animate-spin" />
              ) : idvId ? (
                <Check className="size-4" />
              ) : null}
              {idvId ? "대표자 본인인증 완료" : "대표자 본인인증"}
            </button>
          )}
          {!identityConfigured && (
            <div className="flex items-center gap-2 rounded-xl bg-muted px-3 py-2.5 text-[12px] text-muted-foreground">
              <AlertCircle className="size-4 shrink-0" />
              대표자 본인인증 설정이 필요합니다. (관리자 문의)
            </div>
          )}

          <Field label="사업자등록증">
            <input
              type="file"
              accept="image/*,application/pdf"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              required
              className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm file:mr-3 file:rounded-lg file:border-0 file:bg-muted file:px-3 file:py-1.5 file:text-xs file:font-semibold"
            />
          </Field>
          {error && (
            <div className="flex items-center gap-2 rounded-xl bg-destructive-subtle px-3 py-2.5 text-sm font-medium text-destructive-subtle-foreground">
              <AlertCircle className="size-4 shrink-0" />
              {error}
            </div>
          )}
          <Button type="submit" size="lg" className="w-full" disabled={busy || !ready}>
            {busy && <Loader2 className="animate-spin" />}
            인증 요청하고 만들기
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}
