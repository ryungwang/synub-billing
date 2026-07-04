"use client";

import * as React from "react";
import Link from "next/link";
import { Loader2, AlertCircle, ShieldCheck, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Logo } from "@/components/logo";
import { useAuth } from "@/lib/auth";
import { cn } from "@/lib/utils";
import { COMPANY } from "@/lib/company";

const DEMO = { email: "demo@synub.io", password: "demo1234" };

/** 로그인 하드게이트 — 비로그인 시 전체화면. 통합계정(SSO)으로 로그인/가입, 또는 데모 체험. */
export function LoginScreen() {
  const { login, register } = useAuth();
  const [mode, setMode] = React.useState<"login" | "register">("login");
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [name, setName] = React.useState("");
  const [busy, setBusy] = React.useState<null | "form" | "demo">(null);
  const [error, setError] = React.useState<string | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy("form");
    setError(null);
    try {
      if (mode === "login") await login(email, password);
      else await register(email, password, name);
      window.location.reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "요청에 실패했습니다.");
      setBusy(null);
    }
  }

  async function demo() {
    setBusy("demo");
    setError(null);
    try {
      await login(DEMO.email, DEMO.password);
      window.location.reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "데모 로그인에 실패했습니다.");
      setBusy(null);
    }
  }

  return (
    <div className="flex min-h-dvh items-center justify-center bg-muted/40 px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-7 flex flex-col items-center text-center">
          <Logo size={128} className="size-16" />
          <h1 className="mt-4 text-2xl font-extrabold tracking-tight">
            Synub Billing
          </h1>
          <p className="mt-1.5 text-sm text-muted-foreground">
            통합계정 하나로 Synub의 모든 서비스를 구독·관리하세요.
          </p>
        </div>

        <div className="rounded-3xl border border-border bg-card p-6 shadow-sm sm:p-8">
          <div className="mb-5 grid grid-cols-2 gap-1 rounded-xl bg-muted p-1">
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
              <Field label="이름" value={name} onChange={setName} type="text" placeholder="홍길동" autoComplete="name" />
            )}
            <Field
              label="이메일"
              value={email}
              onChange={setEmail}
              type={mode === "register" ? "email" : "text"}
              placeholder="you@synub.io"
              autoComplete={mode === "register" ? "email" : "username"}
              required
            />
            <Field label="비밀번호" value={password} onChange={setPassword} type="password" placeholder={mode === "register" ? "8자 이상" : "비밀번호"} autoComplete={mode === "login" ? "current-password" : "new-password"} required />

            {error && (
              <div className="flex items-center gap-2 rounded-xl bg-destructive-subtle px-3 py-2.5 text-sm font-medium text-destructive-subtle-foreground">
                <AlertCircle className="size-4 shrink-0" />
                {error}
              </div>
            )}

            <Button type="submit" size="lg" className="w-full" disabled={busy !== null}>
              {busy === "form" && <Loader2 className="animate-spin" />}
              {mode === "login" ? "로그인" : "가입하고 시작하기"}
            </Button>
          </form>

          <div className="my-5 flex items-center gap-3 text-xs text-muted-foreground">
            <div className="h-px flex-1 bg-border" />
            또는
            <div className="h-px flex-1 bg-border" />
          </div>

          <Button variant="outline" size="lg" className="w-full" onClick={demo} disabled={busy !== null}>
            {busy === "demo" ? <Loader2 className="animate-spin" /> : <Sparkles />}
            데모 계정으로 둘러보기
          </Button>
        </div>

        <p className="mt-5 flex items-center justify-center gap-1.5 text-[12px] text-muted-foreground">
          <ShieldCheck className="size-3.5 text-success" />
          비밀번호는 안전하게 암호화되어 보관됩니다.
        </p>

        <div className="mt-6 text-center text-[11px] leading-relaxed text-muted-foreground">
          <div className="flex flex-wrap items-center justify-center gap-x-3 gap-y-1 font-medium">
            <Link href="/pricing" className="hover:text-foreground">요금</Link>
            <span className="text-border">·</span>
            <Link href="/terms" className="hover:text-foreground">이용약관</Link>
            <span className="text-border">·</span>
            <Link href="/privacy" className="font-semibold hover:text-foreground">개인정보처리방침</Link>
            <span className="text-border">·</span>
            <Link href="/refund" className="hover:text-foreground">환불·청약철회</Link>
          </div>
          <div className="mx-auto mt-4 max-w-sm space-y-1 border-t border-border/60 pt-4 text-muted-foreground/70">
            {/* 사업자정보 — 각 항목은 줄바꿈되지 않고(번호 중간 끊김 방지) 항목 사이에서만 줄바꿈 */}
            <div className="flex flex-wrap items-center justify-center gap-x-2 gap-y-0.5">
              {[
                COMPANY.legalName,
                `대표 ${COMPANY.ceo}`,
                `사업자등록번호 ${COMPANY.bizRegNo}`,
                `통신판매업 ${COMPANY.mailOrderNo}`,
              ].map((item, i) => (
                <React.Fragment key={item}>
                  {i > 0 && <span className="text-border">·</span>}
                  <span className="whitespace-nowrap">{item}</span>
                </React.Fragment>
              ))}
            </div>
            <p>{COMPANY.address}</p>
            <div className="flex flex-wrap items-center justify-center gap-x-2">
              <a
                href={`tel:${COMPANY.tel.replace(/[^0-9+]/g, "")}`}
                className="whitespace-nowrap hover:text-foreground"
              >
                고객센터 {COMPANY.tel}
              </a>
              <span className="text-border">·</span>
              <a
                href={`mailto:${COMPANY.email}`}
                className="whitespace-nowrap hover:text-foreground"
              >
                {COMPANY.email}
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
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
      <span className="mb-1 block text-[13px] font-semibold text-foreground">{label}</span>
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
