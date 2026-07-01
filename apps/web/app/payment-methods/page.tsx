"use client";

import * as React from "react";
import {
  Plus,
  Star,
  Trash2,
  ShieldCheck,
  Wifi,
  CheckCircle2,
  Loader2,
  AlertCircle,
} from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card as UICard } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose,
  DialogTrigger,
} from "@/components/ui/dialog";
import { api, type ApiCard } from "@/lib/api";
import { issueBillingKey, portoneConfigured } from "@/lib/portone";
import { useAuth } from "@/lib/auth";
import { cn } from "@/lib/utils";

const BRAND_COLOR: Record<string, string> = {
  신한카드: "#1b64da",
  현대카드: "#111827",
  삼성카드: "#1428a0",
  KB국민카드: "#5a5a3c",
  롯데카드: "#da2128",
};
function brandColor(company: string) {
  return BRAND_COLOR[company] ?? "#3182f6";
}

export default function PaymentMethodsPage() {
  const [cards, setCards] = React.useState<ApiCard[] | null>(null);
  const [deleteTarget, setDeleteTarget] = React.useState<ApiCard | null>(null);
  const [deleteError, setDeleteError] = React.useState<string | null>(null);
  const [busy, setBusy] = React.useState(false);

  const reload = React.useCallback(() => {
    api.cards().then(setCards).catch(() => setCards([]));
  }, []);

  React.useEffect(() => reload(), [reload]);

  async function setPrimary(id: number) {
    setBusy(true);
    try {
      await api.setPrimaryCard(id);
      reload();
    } finally {
      setBusy(false);
    }
  }

  async function confirmDelete() {
    if (!deleteTarget) return;
    setBusy(true);
    setDeleteError(null);
    try {
      await api.deleteCard(deleteTarget.id);
      setDeleteTarget(null);
      reload();
    } catch (e) {
      setDeleteError(e instanceof Error ? e.message : "삭제에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <PageHeader
        title="결제수단"
        description="자동결제에 사용할 카드를 관리하세요. 대표 카드로 모든 구독이 청구됩니다."
        action={<AddCardDialog onAdded={reload} />}
      />

      {cards === null && (
        <div className="flex items-center justify-center gap-2 py-24 text-muted-foreground">
          <Loader2 className="size-5 animate-spin" />
          불러오는 중…
        </div>
      )}

      <div className="grid gap-5 sm:grid-cols-2">
        {cards?.map((c) => (
          <UICard key={c.id} className="overflow-hidden p-0">
            <div
              className="relative h-44 p-5 text-white"
              style={{
                background: `linear-gradient(135deg, ${brandColor(
                  c.company
                )} 0%, color-mix(in srgb, ${brandColor(
                  c.company
                )} 70%, #000) 100%)`,
              }}
            >
              <div className="flex items-start justify-between">
                <span className="text-sm font-bold tracking-wide opacity-95">
                  {c.company}
                </span>
                {c.isPrimary && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-white/20 px-2 py-0.5 text-[11px] font-bold backdrop-blur-sm">
                    <Star className="size-3 fill-current" />
                    대표
                  </span>
                )}
              </div>
              <Wifi className="mt-4 size-6 rotate-90 opacity-80" />
              <div className="mt-3 font-mono text-lg tracking-[0.2em] tnum">
                ···· ···· ···· {c.last4}
              </div>
              <div className="mt-3 flex items-end justify-between">
                <div>
                  <div className="text-[10px] uppercase opacity-70">
                    자동결제
                  </div>
                  <div className="font-mono text-sm">SYNUB BILLING</div>
                </div>
                <div className="text-[11px] font-semibold opacity-90">
                  {c.type}카드
                </div>
              </div>
            </div>

            <div className="flex items-center justify-between px-4 pt-3 text-[13px]">
              <span className="text-muted-foreground">
                이 카드로 청구 중인 구독
              </span>
              <span className="font-bold tnum">{c.billedCount}건</span>
            </div>

            <div className="flex items-center gap-2 p-3 pt-2">
              {c.isPrimary ? (
                <span className="flex flex-1 items-center justify-center gap-1.5 text-[13px] font-semibold text-success-foreground">
                  <CheckCircle2 className="size-4" />
                  대표 결제수단
                </span>
              ) : (
                <Button
                  variant="ghost"
                  size="sm"
                  className="flex-1"
                  disabled={busy}
                  onClick={() => setPrimary(c.id)}
                >
                  <Star />
                  대표로 설정
                </Button>
              )}
              <Button
                variant="ghost"
                size="icon-sm"
                className="text-muted-foreground hover:text-destructive"
                onClick={() => {
                  setDeleteError(null);
                  setDeleteTarget(c);
                }}
                disabled={c.isPrimary}
                aria-label="카드 삭제"
              >
                <Trash2 className="size-[18px]" />
              </Button>
            </div>
          </UICard>
        ))}

        {cards !== null && (
          <AddCardDialog
            onAdded={reload}
            trigger={
              <button className="flex h-[258px] flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed border-border text-muted-foreground transition-colors hover:border-primary/50 hover:bg-primary-subtle/30 hover:text-primary">
                <span className="flex size-12 items-center justify-center rounded-full bg-muted">
                  <Plus className="size-6" />
                </span>
                <span className="text-sm font-semibold">새 카드 등록</span>
              </button>
            }
          />
        )}
      </div>

      <p className="mt-6 flex items-center gap-2 text-[13px] text-muted-foreground">
        <ShieldCheck className="size-4 text-success" />
        카드 번호는 저장하지 않습니다. 빌링키만 안전하게 보관하며 PCI-DSS 표준을
        준수합니다.
      </p>

      <Dialog
        open={!!deleteTarget}
        onOpenChange={(v) => !v && setDeleteTarget(null)}
      >
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <div className="mb-1 flex size-12 items-center justify-center rounded-full bg-destructive-subtle text-destructive">
              <Trash2 className="size-6" />
            </div>
            <DialogTitle>카드를 삭제할까요?</DialogTitle>
            <DialogDescription>
              {deleteTarget?.company} ····{deleteTarget?.last4} 카드가
              삭제됩니다. 이 카드로 청구되는 구독이 없는지 확인해 주세요.
            </DialogDescription>
          </DialogHeader>
          {deleteError && (
            <div className="flex items-center gap-2 rounded-xl bg-destructive-subtle px-3 py-2.5 text-sm font-medium text-destructive-subtle-foreground">
              <AlertCircle className="size-4 shrink-0" />
              {deleteError}
            </div>
          )}
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">취소</Button>
            </DialogClose>
            <Button variant="destructive" onClick={confirmDelete} disabled={busy}>
              {busy && <Loader2 className="animate-spin" />}
              삭제
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}

function AddCardDialog({
  trigger,
  onAdded,
}: {
  trigger?: React.ReactNode;
  onAdded: () => void;
}) {
  const { user } = useAuth();
  const [open, setOpen] = React.useState(false);
  const [phone, setPhone] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const phoneDigits = phone.replace(/[^0-9]/g, "");

  async function register() {
    if (portoneConfigured && phoneDigits.length < 10) {
      setError("휴대폰 번호를 입력하세요. (PG 결제에 필요)");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      if (portoneConfigured) {
        // 실연동: 포트원 결제창에서 카드 빌링키 발급 → 백엔드에 등록(로그인 사용자 정보 사용)
        const issued = await issueBillingKey({
          email: user?.email ?? "",
          fullName: user?.name,
          phoneNumber: phoneDigits,
        });
        if (!issued) throw new Error("포트원 설정이 올바르지 않습니다.");
        await api.registerCard({
          pgBillingKey: issued.billingKey,
          phone: phoneDigits,
          primary: false,
        });
      } else {
        // 데모 폴백: 포트원 미설정 시 임의 카드 등록
        const companies = ["삼성카드", "KB국민카드", "롯데카드"];
        const company = companies[Math.floor(Math.random() * companies.length)];
        const last4 = String(Math.floor(1000 + Math.random() * 9000));
        await api.registerCard({
          pgBillingKey: `demo_billing_key_${Date.now()}`,
          cardCompany: company,
          cardLast4: last4,
          cardType: "신용",
          phone: phoneDigits || undefined,
          primary: false,
        });
      }
      setOpen(false);
      onAdded();
    } catch (e) {
      setError(e instanceof Error ? e.message : "카드 등록에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {trigger ?? (
          <Button>
            <Plus />
            카드 추가
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>새 카드 등록</DialogTitle>
          <DialogDescription>
            포트원(PortOne) 결제창에서 카드를 안전하게 등록합니다.
          </DialogDescription>
        </DialogHeader>
        <div className="rounded-2xl border border-dashed border-border bg-muted/40 p-8 text-center">
          <ShieldCheck className="mx-auto size-10 text-primary" />
          <p className="mt-3 text-sm font-semibold">포트원 결제창으로 이동</p>
          <p className="mt-1 text-xs text-muted-foreground">
            토스페이먼츠를 통해 카드 등록·빌링키 발급이 진행됩니다.
          </p>
        </div>
        <label className="block">
          <span className="mb-1 block text-[13px] font-semibold">휴대폰 번호</span>
          <input
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="01012345678"
            inputMode="numeric"
            className="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm outline-none transition-colors placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20"
          />
          <span className="mt-1 block text-[11px] text-muted-foreground">
            정기결제 청구 시 PG사에 전달됩니다.
          </span>
        </label>
        {error && (
          <div className="flex items-center gap-2 rounded-xl bg-destructive-subtle px-3 py-2.5 text-sm font-medium text-destructive-subtle-foreground">
            <AlertCircle className="size-4 shrink-0" />
            {error}
          </div>
        )}
        <Button size="lg" className="w-full" onClick={register} disabled={busy}>
          {busy && <Loader2 className="animate-spin" />}
          카드 등록하기
        </Button>
      </DialogContent>
    </Dialog>
  );
}
