"use client";

import * as React from "react";
import Link from "next/link";
import { LogOut, Loader2, AlertCircle, ShieldCheck, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { useAuth } from "@/lib/auth";
import { cn } from "@/lib/utils";

export function AuthMenu() {
  const { user, logout } = useAuth();
  const [dialogOpen, setDialogOpen] = React.useState(false);
  const [menuOpen, setMenuOpen] = React.useState(false);

  if (!user) {
    return (
      <>
        <Button size="sm" onClick={() => setDialogOpen(true)}>
          로그인
        </Button>
        <AuthDialog open={dialogOpen} onOpenChange={setDialogOpen} />
      </>
    );
  }

  const label = user.name ?? user.email;
  const initial = (user.name?.[0] ?? user.email[0] ?? "?").toUpperCase();

  return (
    <div className="relative">
      <button
        onClick={() => setMenuOpen((v) => !v)}
        className="flex items-center gap-2.5 rounded-full border border-border bg-card py-1 pl-1 pr-3 transition-colors hover:bg-muted"
      >
        <div className="flex size-7 items-center justify-center rounded-full bg-primary-subtle text-xs font-bold text-primary-subtle-foreground">
          {initial}
        </div>
        <div className="hidden text-left leading-tight sm:block">
          <div className="text-[13px] font-bold">{label}</div>
          <div className="text-[11px] text-muted-foreground">{user.email}</div>
        </div>
      </button>

      {menuOpen && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setMenuOpen(false)} />
          <div className="absolute right-0 top-[calc(100%+8px)] z-50 w-52 overflow-hidden rounded-2xl border border-border bg-card shadow-lg animate-in fade-in-0 slide-in-from-top-1">
            <div className="border-b border-border px-4 py-3">
              <div className="text-sm font-bold">{label}</div>
              <div className="text-xs text-muted-foreground">{user.email}</div>
            </div>
            <Link
              href="/account"
              onClick={() => setMenuOpen(false)}
              className="flex w-full items-center gap-2 border-b border-border px-4 py-3 text-sm font-medium transition-colors hover:bg-muted"
            >
              <User className="size-4" />
              마이페이지
            </Link>
            <button
              onClick={() => {
                logout();
                window.location.reload();
              }}
              className="flex w-full items-center gap-2 px-4 py-3 text-sm font-medium text-destructive transition-colors hover:bg-destructive-subtle"
            >
              <LogOut className="size-4" />
              로그아웃
            </button>
          </div>
        </>
      )}
    </div>
  );
}

function AuthDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const { login, register } = useAuth();
  const [mode, setMode] = React.useState<"login" | "register">("login");
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [name, setName] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    if (!open) {
      setError(null);
      setBusy(false);
    }
  }, [open]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (mode === "login") {
        await login(email, password);
      } else {
        await register(email, password, name);
      }
      onOpenChange(false);
      window.location.reload(); // 토큰 반영해 전체 데이터 갱신
    } catch (err) {
      setError(err instanceof Error ? err.message : "요청에 실패했습니다.");
      setBusy(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{mode === "login" ? "로그인" : "회원가입"}</DialogTitle>
          <DialogDescription>
            신업 통합계정(SSO) 하나로 모든 서비스를 이용하세요.
          </DialogDescription>
        </DialogHeader>

        <div className="mb-1 grid grid-cols-2 gap-1 rounded-xl bg-muted p-1">
          {(["login", "register"] as const).map((m) => (
            <button
              key={m}
              type="button"
              onClick={() => {
                setMode(m);
                setError(null);
              }}
              className={cn(
                "rounded-lg py-2 text-sm font-bold transition-colors",
                mode === m
                  ? "bg-card text-foreground shadow-sm"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              {m === "login" ? "로그인" : "회원가입"}
            </button>
          ))}
        </div>

        <form onSubmit={submit} className="space-y-3">
          {mode === "register" && (
            <Field
              label="이름"
              value={name}
              onChange={setName}
              type="text"
              placeholder="홍길동"
              autoComplete="name"
            />
          )}
          <Field
            label="이메일"
            value={email}
            onChange={setEmail}
            type="email"
            placeholder="you@synub.io"
            autoComplete="email"
            required
          />
          <Field
            label="비밀번호"
            value={password}
            onChange={setPassword}
            type="password"
            placeholder={mode === "register" ? "8자 이상" : "비밀번호"}
            autoComplete={mode === "login" ? "current-password" : "new-password"}
            required
          />

          {error && (
            <div className="flex items-center gap-2 rounded-xl bg-destructive-subtle px-3 py-2.5 text-sm font-medium text-destructive-subtle-foreground">
              <AlertCircle className="size-4 shrink-0" />
              {error}
            </div>
          )}

          <Button type="submit" size="lg" className="w-full" disabled={busy}>
            {busy && <Loader2 className="animate-spin" />}
            {mode === "login" ? "로그인" : "가입하고 시작하기"}
          </Button>
        </form>

        <p className="flex items-center justify-center gap-1.5 text-[12px] text-muted-foreground">
          <ShieldCheck className="size-3.5 text-success" />
          비밀번호는 안전하게 암호화되어 보관됩니다.
        </p>
      </DialogContent>
    </Dialog>
  );
}

function Field({
  label,
  value,
  onChange,
  type,
  placeholder,
  autoComplete,
  required,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  type: string;
  placeholder?: string;
  autoComplete?: string;
  required?: boolean;
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-[13px] font-semibold text-foreground">
        {label}
      </span>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        autoComplete={autoComplete}
        required={required}
        className="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm outline-none transition-colors placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20"
      />
    </label>
  );
}
