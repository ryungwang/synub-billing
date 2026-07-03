"use client";

import * as React from "react";
import { Logo } from "@/components/logo";
import { LoginScreen } from "@/components/login-screen";

/**
 * 서비스 준비 중(닫힘) 화면 — 비로그인 방문자에게 노출.
 * 운영자는 하단 "운영자 로그인"으로 로그인해 빌링을 계속 사용한다(로그인 성공 시 정상 진입).
 * PG(카카오페이/카드사) 심사 요건이 갖춰질 때까지 공개엔 닫아둔다.
 */
export function Maintenance() {
  const [showLogin, setShowLogin] = React.useState(false);

  if (showLogin) return <LoginScreen />;

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-5 bg-muted/30 px-6 text-center">
      <Logo size={128} className="size-16" />
      <div className="space-y-1.5">
        <h1 className="text-2xl font-extrabold tracking-tight">
          서비스 준비 중입니다
        </h1>
        <p className="text-sm text-muted-foreground">
          정식 오픈을 준비하고 있어요. 곧 만나요.
        </p>
      </div>
      <button
        type="button"
        onClick={() => setShowLogin(true)}
        className="mt-1 text-xs font-medium text-muted-foreground/60 transition-colors hover:text-foreground"
      >
        운영자 로그인
      </button>
    </div>
  );
}
