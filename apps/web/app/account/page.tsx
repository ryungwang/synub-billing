"use client";

import * as React from "react";
import Link from "next/link";
import {
  Building2,
  Repeat,
  CreditCard,
  ReceiptText,
  Sparkles,
  LogOut,
  ShieldCheck,
  ChevronRight,
  Check,
  User,
  Loader2,
  Camera,
} from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { api, type ApiOrg } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import {
  setContext,
  rawContext,
  subscribeContext,
  contextOrgId,
} from "@/lib/context";
import { cn } from "@/lib/utils";

const ROLE_LABEL: Record<string, string> = {
  owner: "소유자",
  billing_manager: "결제 관리자",
  member: "멤버",
};

const VERIFY: Record<string, { label: string; cls: string }> = {
  verified: { label: "인증 완료", cls: "bg-success-subtle text-success-foreground" },
  pending: { label: "심사 중", cls: "bg-warning-subtle text-warning-foreground" },
  rejected: { label: "반려", cls: "bg-destructive-subtle text-destructive-subtle-foreground" },
};

const QUICK = [
  { href: "/subscriptions", label: "구독 관리", desc: "구독 중인 제품·플랜", icon: Repeat },
  { href: "/payment-methods", label: "결제수단", desc: "카드 등록·관리", icon: CreditCard },
  { href: "/payments", label: "결제 내역", desc: "영수증·환불", icon: ReceiptText },
  { href: "/products", label: "제품 둘러보기", desc: "새 서비스 구독", icon: Sparkles },
];

export default function AccountPage() {
  const { user, logout } = useAuth();
  const [orgs, setOrgs] = React.useState<ApiOrg[] | null>(null);
  const [avatarUrl, setAvatarUrl] = React.useState<string | null>(null);
  const [uploading, setUploading] = React.useState(false);
  const [err, setErr] = React.useState<string | null>(null);
  const ctx = React.useSyncExternalStore(
    subscribeContext,
    rawContext,
    () => "personal"
  );
  const currentOrgId = contextOrgId(ctx);

  React.useEffect(() => {
    api.organizations().then(setOrgs).catch(() => setOrgs([]));
    api.getProfile().then((p) => setAvatarUrl(p.avatarUrl)).catch(() => {});
  }, []);

  if (!user) return null;
  const initial = (user.name?.[0] ?? user.email[0] ?? "?").toUpperCase();

  function switchTo(value: string) {
    setContext(value);
    window.location.href = "/";
  }

  async function onPickAvatar(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setUploading(true);
    setErr(null);
    try {
      const p = await api.uploadAvatar(file);
      setAvatarUrl(p.avatarUrl);
    } catch (e2) {
      setErr(e2 instanceof Error ? e2.message : "업로드에 실패했습니다.");
    } finally {
      setUploading(false);
    }
  }

  async function onDeleteAvatar() {
    setUploading(true);
    setErr(null);
    try {
      await api.deleteAvatar();
      setAvatarUrl(null);
    } catch (e2) {
      setErr(e2 instanceof Error ? e2.message : "삭제에 실패했습니다.");
    } finally {
      setUploading(false);
    }
  }

  return (
    <>
      <PageHeader
        title="마이페이지"
        description="통합계정 정보와 소속 회사를 확인하고 관리하세요."
      />

      {/* 프로필 */}
      <Card className="p-6">
        <div className="flex flex-wrap items-center gap-4">
          <div className="relative shrink-0">
            <div className="flex size-16 items-center justify-center overflow-hidden rounded-2xl bg-primary-subtle text-2xl font-extrabold text-primary-subtle-foreground">
              {avatarUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={avatarUrl}
                  alt="프로필"
                  className="size-full object-cover"
                />
              ) : (
                initial
              )}
            </div>
            <label
              title="프로필 사진 변경"
              className="absolute -bottom-1.5 -right-1.5 flex size-7 cursor-pointer items-center justify-center rounded-full border-2 border-card bg-primary text-primary-foreground shadow-sm transition-colors hover:bg-primary/90"
            >
              {uploading ? (
                <Loader2 className="size-3.5 animate-spin" />
              ) : (
                <Camera className="size-3.5" />
              )}
              <input
                type="file"
                accept="image/png,image/jpeg"
                className="hidden"
                onChange={onPickAvatar}
                disabled={uploading}
              />
            </label>
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-lg font-extrabold">
                {user.name ?? user.email}
              </span>
              {user.admin && (
                <Badge>
                  <ShieldCheck className="size-3" /> 관리자
                </Badge>
              )}
            </div>
            <div className="mt-0.5 text-sm text-muted-foreground">
              {user.email}
            </div>
            {avatarUrl && (
              <button
                onClick={onDeleteAvatar}
                disabled={uploading}
                className="mt-1 text-xs font-medium text-muted-foreground transition-colors hover:text-destructive disabled:opacity-50"
              >
                프로필 사진 삭제
              </button>
            )}
            {err && <div className="mt-1 text-xs text-destructive">{err}</div>}
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              logout();
              window.location.href = "/";
            }}
          >
            <LogOut className="size-4" />
            로그아웃
          </Button>
        </div>
      </Card>

      {/* 소속 회사 */}
      <section className="mt-8">
        <h2 className="mb-3 text-sm font-bold">소속 회사</h2>
        <Card className="overflow-hidden p-0">
          <ContextRow
            active={currentOrgId === null}
            onClick={() => switchTo("personal")}
            icon={<User className="size-4 text-muted-foreground" />}
            title="개인"
            subtitle="개인 계정으로 구독·결제"
          />
          {orgs === null ? (
            <div className="flex items-center justify-center gap-2 border-t border-border py-8 text-sm text-muted-foreground">
              <Loader2 className="size-4 animate-spin" /> 불러오는 중…
            </div>
          ) : orgs.length === 0 ? (
            <div className="border-t border-border px-5 py-8 text-center text-sm text-muted-foreground">
              소속된 회사가 없습니다. 상단 회사 전환에서 회사를 만들 수 있어요.
            </div>
          ) : (
            orgs.map((o) => {
              const v = VERIFY[o.verifyStatus] ?? VERIFY.pending;
              return (
                <ContextRow
                  key={o.id}
                  active={currentOrgId === o.id}
                  onClick={() => switchTo(`org:${o.id}`)}
                  icon={<Building2 className="size-4 text-muted-foreground" />}
                  title={o.name}
                  badges={
                    <>
                      <span className="rounded-md bg-secondary px-1.5 py-0.5 text-[11px] font-bold text-secondary-foreground">
                        {ROLE_LABEL[o.role] ?? o.role}
                      </span>
                      <span
                        className={cn(
                          "rounded-md px-1.5 py-0.5 text-[11px] font-bold",
                          v.cls
                        )}
                      >
                        {v.label}
                      </span>
                    </>
                  }
                  subtitle={o.orgCode ? `조직코드 ${o.orgCode}` : undefined}
                />
              );
            })
          )}
        </Card>
        <p className="mt-2 text-xs text-muted-foreground">
          회사를 선택하면 해당 회사 컨텍스트로 전환됩니다.
        </p>
      </section>

      {/* 빠른 관리 */}
      <section className="mt-8">
        <h2 className="mb-3 text-sm font-bold">빠른 관리</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          {QUICK.map((q) => (
            <Link
              key={q.href}
              href={q.href}
              className="group flex items-center gap-3 rounded-2xl border border-border bg-card p-4 transition-all hover:border-primary/40 hover:shadow-[var(--shadow-card-hover)]"
            >
              <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary-subtle text-primary">
                <q.icon className="size-5" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="text-sm font-bold">{q.label}</div>
                <div className="text-xs text-muted-foreground">{q.desc}</div>
              </div>
              <ChevronRight className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
            </Link>
          ))}
        </div>
      </section>
    </>
  );
}

function ContextRow({
  active,
  onClick,
  icon,
  title,
  subtitle,
  badges,
}: {
  active: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  title: string;
  subtitle?: string;
  badges?: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "flex w-full items-center gap-3 border-t border-border px-5 py-4 text-left transition-colors first:border-t-0 hover:bg-muted/40",
        active && "bg-primary-subtle/40"
      )}
    >
      <div className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-muted">
        {icon}
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="truncate text-sm font-bold">{title}</span>
          {badges}
        </div>
        {subtitle && (
          <div className="mt-0.5 truncate text-xs tnum text-muted-foreground">
            {subtitle}
          </div>
        )}
      </div>
      {active ? (
        <span className="flex items-center gap-1 text-xs font-bold text-primary">
          <Check className="size-4" /> 현재
        </span>
      ) : (
        <span className="text-xs font-medium text-muted-foreground">전환</span>
      )}
    </button>
  );
}
