"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { ProductIcon } from "@/components/product-icon";
import { cn, formatKRW } from "@/lib/utils";
import { api, type ApiCard, type PricingType } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { issueBillingKey, portoneConfigured } from "@/lib/portone";
import {
  Check,
  CreditCard,
  Plus,
  ShieldCheck,
  PartyPopper,
  ExternalLink,
  Loader2,
  AlertCircle,
  Minus,
  Users,
} from "lucide-react";

export interface CheckoutTarget {
  planId: number;
  product: string;
  plan: string;
  amount: number; // 정액=총액, per_seat=1인당 단가
  cycle: "monthly" | "yearly";
  pricingType: PricingType;
}

export function CheckoutDialog({
  target,
  open,
  onOpenChange,
}: {
  target: CheckoutTarget | null;
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const router = useRouter();
  const { user } = useAuth();
  const [cards, setCards] = React.useState<ApiCard[]>([]);
  const [selectedCard, setSelectedCard] = React.useState<number | "new" | null>(
    null
  );
  const [agreed, setAgreed] = React.useState(false);
  const [done, setDone] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [seats, setSeats] = React.useState(1);
  const [idemKey, setIdemKey] = React.useState("");

  React.useEffect(() => {
    if (!open) return;
    setDone(false);
    setAgreed(false);
    setError(null);
    setSeats(1);
    // 이 결제 시도의 멱등키 — 재시도(네트워크 오류 등) 시 재사용해 이중 청구 방지
    setIdemKey(crypto.randomUUID());
    api
      .cards()
      .then((cs) => {
        setCards(cs);
        setSelectedCard(cs.find((c) => c.isPrimary)?.id ?? cs[0]?.id ?? "new");
      })
      .catch((e) => setError(e.message));
  }, [open]);

  function close(refresh: boolean) {
    onOpenChange(false);
    if (refresh) router.refresh();
  }

  async function submit() {
    if (!target) return;
    setSubmitting(true);
    setError(null);
    try {
      let cardId =
        typeof selectedCard === "number" ? selectedCard : undefined;
      if (selectedCard === "new") {
        // 새 카드: 포트원 결제창에서 빌링키 발급 → 등록 → 그 카드로 구독.
        if (!portoneConfigured) throw new Error("포트원 결제 설정이 필요합니다.");
        const issued = await issueBillingKey({
          email: user?.email ?? "",
          fullName: user?.name,
        });
        if (!issued) throw new Error("카드 등록에 실패했습니다.");
        const card = await api.registerCard({
          pgBillingKey: issued.billingKey,
          primary: true,
        });
        cardId = card.id;
      }
      if (!cardId) cardId = cards.find((c) => c.isPrimary)?.id;
      if (!cardId) throw new Error("결제할 카드를 먼저 등록해 주세요.");
      await api.createSubscription(
        target.planId,
        cardId,
        target.pricingType === "per_seat" ? seats : undefined,
        idemKey
      );
      setDone(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : "결제에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  if (!target) return null;
  const cycleLabel = target.cycle === "yearly" ? "년" : "월";
  const perSeat = target.pricingType === "per_seat";
  const total = perSeat ? target.amount * seats : target.amount;

  return (
    <Dialog open={open} onOpenChange={(v) => close(v ? true : done)}>
      <DialogContent className="max-w-md">
        {done ? (
          <div className="flex flex-col items-center py-4 text-center">
            <div className="flex size-16 items-center justify-center rounded-full bg-success-subtle text-success">
              <PartyPopper className="size-8" />
            </div>
            <DialogTitle className="mt-5 text-xl">
              구독이 시작됐어요!
            </DialogTitle>
            <DialogDescription className="mt-2">
              {target.product} {target.plan} 플랜이 활성화됐습니다.
              <br />
              영수증은 등록된 이메일로 발송됩니다.
            </DialogDescription>
            <div className="mt-5 w-full rounded-2xl bg-muted/60 p-4 text-left">
              <Row label="제품" value={target.product} />
              <Row label="플랜" value={target.plan} />
              {perSeat && (
                <Row label="좌석" value={`${seats}인 × ${formatKRW(target.amount)}`} />
              )}
              <Row
                label="결제 금액"
                value={`${formatKRW(total)} / ${cycleLabel}`}
                strong
              />
            </div>
            <Button
              className="mt-5 w-full"
              size="lg"
              onClick={() => close(true)}
            >
              확인
            </Button>
          </div>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>구독 신청</DialogTitle>
              <DialogDescription>
                카드를 선택하고 약관에 동의하면 바로 시작돼요.
              </DialogDescription>
            </DialogHeader>

            <div className="flex items-center gap-3 rounded-2xl border border-border bg-muted/40 p-4">
              <ProductIcon name={target.product} size="lg" />
              <div className="flex-1">
                <div className="font-bold">{target.product}</div>
                <div className="text-sm text-muted-foreground">
                  {target.plan} 플랜
                </div>
              </div>
              <div className="text-right">
                <div className="text-lg font-extrabold tnum">
                  {formatKRW(target.amount)}
                </div>
                <div className="text-xs text-muted-foreground">
                  {perSeat ? `/ 인 · ${cycleLabel}` : `/ ${cycleLabel}`}
                </div>
              </div>
            </div>

            {perSeat && (
              <div className="flex items-center justify-between rounded-2xl border border-border p-4">
                <div className="flex items-center gap-2 text-sm font-bold">
                  <Users className="size-4 text-primary" />
                  좌석 수
                </div>
                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => setSeats((s) => Math.max(1, s - 1))}
                    className="flex size-8 items-center justify-center rounded-lg border border-border hover:bg-muted disabled:opacity-40"
                    disabled={seats <= 1}
                    aria-label="좌석 줄이기"
                  >
                    <Minus className="size-4" />
                  </button>
                  <span className="w-8 text-center text-base font-extrabold tnum">
                    {seats}
                  </span>
                  <button
                    type="button"
                    onClick={() => setSeats((s) => Math.min(999, s + 1))}
                    className="flex size-8 items-center justify-center rounded-lg border border-border hover:bg-muted"
                    aria-label="좌석 늘리기"
                  >
                    <Plus className="size-4" />
                  </button>
                </div>
              </div>
            )}

            <div>
              <div className="mb-2 text-sm font-bold">결제수단</div>
              <div className="space-y-2">
                {cards.map((c) => (
                  <button
                    key={c.id}
                    onClick={() => setSelectedCard(c.id)}
                    className={cn(
                      "flex w-full items-center gap-3 rounded-xl border p-3 text-left transition-all",
                      selectedCard === c.id
                        ? "border-primary bg-primary-subtle/50 ring-2 ring-primary/20"
                        : "border-border hover:bg-muted/50"
                    )}
                  >
                    <CreditCard className="size-5 text-muted-foreground" />
                    <span className="flex-1 text-sm font-semibold">
                      {c.company} ····{c.last4}
                    </span>
                    {c.isPrimary && (
                      <span className="rounded-md bg-secondary px-1.5 py-0.5 text-[11px] font-bold text-secondary-foreground">
                        대표
                      </span>
                    )}
                    {selectedCard === c.id && (
                      <Check className="size-4 text-primary" />
                    )}
                  </button>
                ))}
                <button
                  onClick={() => setSelectedCard("new")}
                  className={cn(
                    "flex w-full items-center gap-3 rounded-xl border border-dashed p-3 text-left text-sm font-semibold transition-all",
                    selectedCard === "new"
                      ? "border-primary bg-primary-subtle/50 text-primary-subtle-foreground"
                      : "border-border text-muted-foreground hover:bg-muted/50"
                  )}
                >
                  <Plus className="size-5" />새 카드로 결제 (포트원 결제창)
                </button>
              </div>
            </div>

            <label className="flex cursor-pointer items-start gap-2.5 text-sm">
              <input
                type="checkbox"
                checked={agreed}
                onChange={(e) => setAgreed(e.target.checked)}
                className="peer sr-only"
              />
              <span
                className={cn(
                  "mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-md border transition-colors",
                  agreed
                    ? "border-primary bg-primary text-primary-foreground"
                    : "border-border bg-card"
                )}
              >
                {agreed && <Check className="size-3.5" strokeWidth={3} />}
              </span>
              <span className="text-muted-foreground">
                <a
                  href="/terms"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-semibold text-foreground underline-offset-2 hover:underline"
                >
                  이용약관
                </a>{" "}
                및{" "}
                <a
                  href="/refund"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-semibold text-foreground underline-offset-2 hover:underline"
                >
                  환불정책
                  <ExternalLink className="ml-0.5 inline size-3" />
                </a>
                에 동의합니다.
              </span>
            </label>

            {error && (
              <div className="flex items-center gap-2 rounded-xl bg-destructive-subtle px-3 py-2.5 text-sm font-medium text-destructive-subtle-foreground">
                <AlertCircle className="size-4 shrink-0" />
                {error}
              </div>
            )}

            <Button
              size="lg"
              className="w-full"
              disabled={!agreed || submitting}
              onClick={submit}
            >
              {submitting && <Loader2 className="animate-spin" />}
              {formatKRW(total)} 결제하고 시작하기
            </Button>
            <p className="flex items-center justify-center gap-1.5 text-xs text-muted-foreground">
              <ShieldCheck className="size-3.5 text-success" />
              카드 정보는 저장하지 않으며 포트원(PortOne)이 안전하게 처리합니다.
            </p>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}

function Row({
  label,
  value,
  strong,
}: {
  label: string;
  value: string;
  strong?: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-1 text-sm">
      <span className="text-muted-foreground">{label}</span>
      <span className={cn(strong ? "font-extrabold" : "font-semibold")}>
        {value}
      </span>
    </div>
  );
}
