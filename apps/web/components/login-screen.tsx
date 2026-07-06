"use client";

import * as React from "react";
import Link from "next/link";
import { Loader2, AlertCircle, Sparkles, Check } from "lucide-react";
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
    // 배경 위에 '2단 분할 전체'가 하나의 떠있는 카드로, 아래에서 슬라이드업하며 등장.
    <div className="flex min-h-dvh flex-col items-center justify-center gap-5 bg-gradient-to-b from-muted/20 via-muted/40 to-muted/70 p-4 sm:p-6">
      <div className="animate-slide-up w-full max-w-5xl overflow-hidden rounded-3xl border border-border/60 shadow-pop">
        <div className="grid md:min-h-[38rem] md:grid-cols-2">
          {/* 왼쪽 — 브랜드 패널(데스크톱 전용) */}
          <div className="dark relative hidden flex-col justify-between bg-gradient-to-br from-[#14233f] to-[#0a0f1c] p-12 text-white md:flex">
            <div className="flex items-center gap-3">
              <Logo size={128} className="size-12" />
              <span className="text-2xl font-extrabold tracking-tight">Synub Billing</span>
            </div>
            <div className="max-w-xs">
              <h2 className="text-[26px] font-extrabold leading-tight tracking-tight">
                통합계정 하나로
                <br />
                모든 SaaS를 한 곳에서.
              </h2>
              <p className="mt-3 text-sm leading-relaxed text-white/60">
                구독·결제·플랜 변경까지 — Synub 하나로 관리하세요.
              </p>
              <ul className="mt-8 space-y-3.5">
                {[
                  "여러 SaaS를 계정 하나로",
                  "카드 한 장으로 결제·청구 관리",
                  "언제든 플랜 변경·해지",
                ].map((t) => (
                  <li key={t} className="flex items-center gap-3 text-sm text-white/85">
                    <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-success/20">
                      <Check className="size-3 text-success" strokeWidth={3} />
                    </span>
                    {t}
                  </li>
                ))}
              </ul>
            </div>
            <Fineprint tone="dark" align="left" />
          </div>

          {/* 오른쪽 — 로그인 */}
          <div className="flex flex-col justify-center bg-card p-7 sm:p-10">
            {/* 모바일 브랜드 헤더 */}
            <div className="mb-5 flex flex-col items-center text-center md:hidden">
              <Logo size={128} className="size-12" />
              <h1 className="mt-2 text-2xl font-extrabold tracking-tight">Synub Billing</h1>
              <p className="mt-1 text-sm text-muted-foreground">
                통합계정 하나로 Synub의 모든 서비스를 구독·관리하세요.
              </p>
            </div>
            {/* 데스크톱 제목 */}
            <div className="mb-5 hidden md:block">
              <h1 className="text-2xl font-extrabold tracking-tight">로그인</h1>
              <p className="mt-1 text-sm text-muted-foreground">
                통합계정으로 Synub 서비스를 이용하세요.
              </p>
            </div>

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

            <form onSubmit={submit} className="space-y-3.5">
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

            <div className="my-4 flex items-center gap-3 text-xs text-muted-foreground">
              <div className="h-px flex-1 bg-border" />
              또는
              <div className="h-px flex-1 bg-border" />
            </div>

            <Button variant="outline" size="lg" className="w-full" onClick={demo} disabled={busy !== null}>
              {busy === "demo" ? <Loader2 className="animate-spin" /> : <Sparkles />}
              데모 계정으로 둘러보기
            </Button>
          </div>
        </div>
      </div>

      {/* 모바일 법적 푸터(데스크톱은 카드 왼쪽 패널 하단이 대신) */}
      <Fineprint tone="light" align="center" className="md:hidden" />
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
          <span className="whitespace-nowrap">전화번호 {COMPANY.tel}</span>
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
        className="w-full rounded-xl border border-border bg-background px-3.5 py-3 text-sm outline-none transition-colors placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20"
      />
    </label>
  );
}
