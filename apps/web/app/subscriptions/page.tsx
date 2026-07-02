"use client";

import * as React from "react";
import Link from "next/link";
import {
  CalendarClock,
  CreditCard,
  CalendarPlus,
  Hourglass,
  Settings2,
  Ban,
  RotateCcw,
  ArrowRight,
  AlertTriangle,
  Loader2,
  Minus,
  Plus,
  Sparkles,
} from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose,
} from "@/components/ui/dialog";
import { SubscriptionStatusBadge } from "@/components/status-badge";
import { ProductIcon } from "@/components/product-icon";
import { UsageBar } from "@/components/usage-bar";
import {
  api,
  type ApiSubscription,
  type ApiProduct,
  type ApiPlan,
} from "@/lib/api";
import { cn, formatKRW, formatDate } from "@/lib/utils";

export default function SubscriptionsPage() {
  const [subs, setSubs] = React.useState<ApiSubscription[] | null>(null);
  const [products, setProducts] = React.useState<ApiProduct[]>([]);
  const [cancelTarget, setCancelTarget] = React.useState<ApiSubscription | null>(
    null
  );
  const [busy, setBusy] = React.useState(false);

  const reload = React.useCallback(() => {
    api.subscriptions().then(setSubs).catch(() => setSubs([]));
  }, []);

  React.useEffect(() => {
    reload();
    api.products().then(setProducts).catch(() => {});
  }, [reload]);

  async function confirmCancel() {
    if (!cancelTarget) return;
    setBusy(true);
    try {
      await api.cancelSubscription(cancelTarget.id);
      setCancelTarget(null);
      reload();
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <PageHeader
        title="구독 관리"
        description="구독 중인 제품을 확인하고 플랜 변경·해지를 관리하세요."
        action={
          <Button asChild variant="outline">
            <Link href="/products">
              새 구독 추가
              <ArrowRight />
            </Link>
          </Button>
        }
      />

      {subs === null && (
        <div className="flex items-center justify-center gap-2 py-24 text-muted-foreground">
          <Loader2 className="size-5 animate-spin" />
          불러오는 중…
        </div>
      )}

      <div className="space-y-4">
        {subs?.map((s) => {
          const isCanceled = s.status === "canceled";
          const isPastDue = s.status === "past_due";
          const planList =
            products.find((p) => p.serviceCode === s.serviceCode)?.plans ?? [];
          return (
            <Card
              key={s.id}
              className={cn("overflow-hidden", isPastDue && "border-warning/40")}
            >
              <div className="p-5 sm:p-6">
                <div className="flex items-start gap-4">
                  <ProductIcon name={s.product} size="xl" />
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-base font-bold">{s.product}</span>
                      <span className="rounded-md bg-secondary px-1.5 py-0.5 text-[11px] font-bold text-secondary-foreground">
                        {s.plan}
                      </span>
                      <SubscriptionStatusBadge status={s.status} />
                    </div>
                    <div className="mt-1 text-[13px] text-muted-foreground">
                      {s.cycle === "yearly" ? "연간 구독" : "월간 구독"} ·{" "}
                      {s.monthsActive}개월째 이용 중
                    </div>
                  </div>
                  <div className="shrink-0 text-right">
                    <div className="text-lg font-extrabold tnum">
                      {s.complimentary ? "무상" : formatKRW(s.amount)}
                    </div>
                    <div className="text-xs text-muted-foreground">
                      {s.complimentary
                        ? "개발사 제공"
                        : s.pricingType === "per_seat"
                        ? `${s.seats}인 × ${formatKRW(s.unitAmount)} / 월`
                        : `/ ${s.cycle === "yearly" ? "년" : "월"}`}
                    </div>
                  </div>
                </div>

                <div className="mt-5 grid gap-x-8 gap-y-4 border-t border-border pt-5 sm:grid-cols-2">
                  {s.usage && (
                    <div>
                      <div className="mb-2 text-[11px] font-bold uppercase tracking-wide text-muted-foreground">
                        이번 달 사용량
                      </div>
                      <UsageBar usage={s.usage} />
                    </div>
                  )}
                  <dl
                    className={cn(
                      "grid grid-cols-2 gap-x-4 gap-y-3 text-sm",
                      !s.usage && "sm:col-span-2"
                    )}
                  >
                    <Fact
                      icon={CalendarPlus}
                      label="시작일"
                      value={formatDate(s.startedAt)}
                    />
                    <Fact
                      icon={isCanceled ? Hourglass : CalendarClock}
                      label={isCanceled ? "이용 종료" : "다음 결제"}
                      value={formatDate(s.nextBillingDate)}
                    />
                    <Fact icon={CreditCard} label="결제수단" value={s.card} />
                    <Fact
                      icon={RotateCcw}
                      label="청구 주기"
                      value={s.cycle === "yearly" ? "매년" : "매월"}
                    />
                  </dl>
                </div>

                {s.pricingType === "per_seat" && !isCanceled && (
                  <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-border pt-4">
                    <div className="text-[13px]">
                      <span className="font-semibold">좌석 {s.seats}석</span>
                      <span className="ml-1.5 text-muted-foreground">
                        · {formatKRW(s.unitAmount)} / 인 · 월
                      </span>
                      {s.creditBalance > 0 && (
                        <span className="ml-2 rounded-md bg-success-subtle px-1.5 py-0.5 text-[11px] font-bold text-success-foreground">
                          크레딧 {formatKRW(s.creditBalance)} · 다음 청구 차감
                        </span>
                      )}
                    </div>
                    <SeatControl sub={s} onChanged={reload} />
                  </div>
                )}
              </div>

              <div className="flex items-center gap-2 border-t border-border bg-muted/30 px-5 py-3 sm:px-6">
                {s.complimentary ? (
                  <div className="flex items-center gap-1.5 text-[13px] font-medium text-success-foreground">
                    <Sparkles className="size-4 text-success" />
                    개발사 무상 제공 · 자동 관리 (청구·해지 없음)
                  </div>
                ) : (
                  <>
                    {isPastDue && (
                      <div className="mr-auto flex items-center gap-1.5 text-[13px] font-medium text-warning-foreground">
                        <AlertTriangle className="size-4 text-warning" />
                        결제 실패 — 재시도 예정
                      </div>
                    )}
                    {isCanceled && (
                      <div className="mr-auto text-[13px] text-muted-foreground">
                        {formatDate(s.nextBillingDate)}까지 이용 후 자동 종료됩니다.
                      </div>
                    )}
                    {!isPastDue && !isCanceled && (
                      <div className="mr-auto text-[13px] text-muted-foreground">
                        다음 청구 예정{" "}
                        <span className="font-semibold text-foreground tnum">
                          {formatKRW(s.amount)}
                        </span>
                      </div>
                    )}
                    {isCanceled ? (
                      <Button variant="subtle" size="sm" disabled>
                        <RotateCcw />
                        해지 예정
                      </Button>
                    ) : (
                      <>
                        <ChangePlanDialog
                          sub={s}
                          plans={planList}
                          onChanged={reload}
                        />
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-muted-foreground hover:text-destructive"
                          onClick={() => setCancelTarget(s)}
                        >
                          <Ban />
                          해지
                        </Button>
                      </>
                    )}
                  </>
                )}
              </div>
            </Card>
          );
        })}
      </div>

      <Dialog
        open={!!cancelTarget}
        onOpenChange={(v) => !v && setCancelTarget(null)}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <div className="mb-1 flex size-12 items-center justify-center rounded-full bg-destructive-subtle text-destructive">
              <Ban className="size-6" />
            </div>
            <DialogTitle>{cancelTarget?.product} 구독을 해지할까요?</DialogTitle>
            <DialogDescription>
              지금 해지해도{" "}
              <span className="font-semibold text-foreground">
                {cancelTarget && formatDate(cancelTarget.nextBillingDate)}
              </span>
              까지는 그대로 이용할 수 있어요. 이후 자동으로 종료되며 추가 청구는
              없습니다.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">계속 이용하기</Button>
            </DialogClose>
            <Button variant="destructive" onClick={confirmCancel} disabled={busy}>
              {busy && <Loader2 className="animate-spin" />}
              기간 만료 시 해지
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}

function Fact({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof CalendarClock;
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-center gap-2.5">
      <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
        <Icon className="size-4" />
      </span>
      <div className="min-w-0">
        <dt className="text-[11px] text-muted-foreground">{label}</dt>
        <dd className="truncate text-[13px] font-semibold">{value}</dd>
      </div>
    </div>
  );
}

function SeatControl({
  sub,
  onChanged,
}: {
  sub: ApiSubscription;
  onChanged: () => void;
}) {
  const [seats, setSeats] = React.useState(sub.seats);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const changed = seats !== sub.seats;

  async function apply() {
    setBusy(true);
    setError(null);
    try {
      await api.changeSeats(sub.id, seats);
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : "변경에 실패했습니다.");
      setSeats(sub.seats);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex items-center gap-2">
      {error && <span className="text-xs font-medium text-destructive">{error}</span>}
      <div className="flex items-center gap-1 rounded-xl border border-border p-1">
        <button
          onClick={() => setSeats((s) => Math.max(1, s - 1))}
          disabled={busy || seats <= 1}
          className="flex size-7 items-center justify-center rounded-lg hover:bg-muted disabled:opacity-40"
          aria-label="좌석 줄이기"
        >
          <Minus className="size-3.5" />
        </button>
        <span className="w-7 text-center text-sm font-bold tnum">{seats}</span>
        <button
          onClick={() => setSeats((s) => Math.min(999, s + 1))}
          disabled={busy}
          className="flex size-7 items-center justify-center rounded-lg hover:bg-muted disabled:opacity-40"
          aria-label="좌석 늘리기"
        >
          <Plus className="size-3.5" />
        </button>
      </div>
      <Button size="sm" onClick={apply} disabled={!changed || busy}>
        {busy && <Loader2 className="animate-spin" />}
        {changed ? `${seats}석 적용` : "좌석 변경"}
      </Button>
    </div>
  );
}

function ChangePlanDialog({
  sub,
  plans,
  onChanged,
}: {
  sub: ApiSubscription;
  plans: ApiPlan[];
  onChanged: () => void;
}) {
  const [open, setOpen] = React.useState(false);
  const [busy, setBusy] = React.useState(false);

  async function change(planId: number) {
    setBusy(true);
    try {
      await api.changePlan(sub.id, planId);
      setOpen(false);
      onChanged();
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <Button variant="outline" size="sm" onClick={() => setOpen(true)}>
        <Settings2 />
        플랜 변경
      </Button>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>플랜 변경</DialogTitle>
          <DialogDescription>
            {sub.product}의 다른 플랜으로 변경합니다. 변경 사항은 다음 결제일부터
            적용돼요.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          {plans.map((p) => {
            const current = p.name === sub.plan;
            return (
              <div
                key={p.id}
                className={cn(
                  "flex items-center gap-3 rounded-xl border p-3.5",
                  current
                    ? "border-primary bg-primary-subtle/40"
                    : "border-border hover:bg-muted/50"
                )}
              >
                <div className="flex-1">
                  <div className="flex items-center gap-2 text-sm font-bold">
                    {p.name}
                    {current && (
                      <span className="rounded bg-primary px-1.5 py-0.5 text-[10px] font-bold text-primary-foreground">
                        현재
                      </span>
                    )}
                  </div>
                  <div className="text-xs text-muted-foreground">
                    {p.tagline}
                  </div>
                </div>
                <div className="text-right text-sm font-bold tnum">
                  {formatKRW(p.amount)}
                  <span className="text-xs font-normal text-muted-foreground">
                    /{p.cycle === "yearly" ? "년" : "월"}
                  </span>
                </div>
                <Button
                  size="sm"
                  variant={current ? "ghost" : "subtle"}
                  disabled={current || busy}
                  onClick={() => change(p.id)}
                >
                  {current ? "이용 중" : "변경"}
                </Button>
              </div>
            );
          })}
        </div>
      </DialogContent>
    </Dialog>
  );
}
