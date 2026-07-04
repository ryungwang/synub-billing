"use client";

import * as React from "react";
import Link from "next/link";
import { Loader2, AlertCircle, Sparkles } from "lucide-react";
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
    // 그라데이션 배경 + 가운데 '떠있는 카드'(슬라이드업). 법적 푸터는 하단 고정(카드 스택에서 분리 → 스크롤 방지).
    <div className="flex min-h-dvh flex-col bg-gradient-to-b from-muted/20 via-muted/40 to-muted/70 px-4">
      <div className="flex flex-1 items-center justify-center py-8">
        <div className="animate-slide-up w-full max-w-md">
          <div className="mb-4 flex flex-col items-center text-center">
            <Logo size={128} className="size-12" />
            <h1 className="mt-2 text-2xl font-extrabold tracking-tight">Synub Billing</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              통합계정 하나로 Synub의 모든 서비스를 구독·관리하세요.
            </p>
          </div>

          {/* 떠있는 카드 — 강한 그림자로 부양감 */}
          <div className="rounded-3xl border border-border/60 bg-card p-5 shadow-pop sm:p-6">
          <div className="mb-4 grid grid-cols-2 gap-1 rounded-xl bg-muted p-1">
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

          <Button variant="outline" size="lg" className="mt-3 w-full" onClick={demo} disabled={busy !== null}>
            {busy === "demo" ? <Loader2 className="animate-spin" /> : <Sparkles />}
            데모 계정으로 둘러보기
          </Button>
          </div>
        </div>
      </div>
      <footer className="pb-5 pt-2">
        <Fineprint tone="light" align="center" className="mx-auto max-w-2xl" />
      </footer>
    </div>
  );
}

/** 요금·약관 링크 + 전자상거래 사업자정보. 밝은/어두운 톤 공용. */
function Fineprint({
  tone,
  align,
  className,
}: {
  tone: "light" | "dark";
  align: "center" | "left";
  className?: string;
}) {
  const linkCls = tone === "dark" ? "text-white/70 hover:text-white" : "text-muted-foreground hover:text-foreground";
  const infoCls = tone === "dark" ? "text-white/45" : "text-muted-foreground/70";
  const sepCls = tone === "dark" ? "text-white/25" : "text-border";
  const borderCls = tone === "dark" ? "border-white/10" : "border-border/60";
  const justify = align === "center" ? "justify-center" : "justify-start";
  return (
    <div className={cn("text-[11px] leading-relaxed", align === "center" ? "text-center" : "text-left", className)}>
      <div className={cn("flex flex-wrap items-center gap-x-3 gap-y-1 font-medium", justify)}>
        <Link href="/pricing" className={linkCls}>요금</Link>
        <span className={sepCls}>·</span>
        <Link href="/terms" className={linkCls}>이용약관</Link>
        <span className={sepCls}>·</span>
        <Link href="/privacy" className={cn("font-semibold", linkCls)}>개인정보처리방침</Link>
        <span className={sepCls}>·</span>
        <Link href="/refund" className={linkCls}>환불·청약철회</Link>
      </div>
      {/* 사업자정보 — 각 항목 whitespace-nowrap(번호 중간 끊김 방지), 항목 사이에서만 줄바꿈 */}
      <div className={cn("mt-3 space-y-1 border-t pt-3", infoCls, borderCls)}>
        <div className={cn("flex flex-wrap items-center gap-x-2 gap-y-0.5", justify)}>
          {[
            COMPANY.legalName,
            `대표 ${COMPANY.ceo}`,
            `사업자등록번호 ${COMPANY.bizRegNo}`,
            `통신판매업 ${COMPANY.mailOrderNo}`,
          ].map((item, i) => (
            <React.Fragment key={item}>
              {i > 0 && <span className={sepCls}>·</span>}
              <span className="whitespace-nowrap">{item}</span>
            </React.Fragment>
          ))}
        </div>
        <p>{COMPANY.address}</p>
        <div className={cn("flex flex-wrap items-center gap-x-2", justify)}>
          <a href={`tel:${COMPANY.tel.replace(/[^0-9+]/g, "")}`} className="whitespace-nowrap hover:underline">
            고객센터 {COMPANY.tel}
          </a>
          <span className={sepCls}>·</span>
          <a href={`mailto:${COMPANY.email}`} className="whitespace-nowrap hover:underline">
            {COMPANY.email}
          </a>
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
