"use client";

import * as React from "react";
import { Logo } from "@/components/logo";
import { LoginScreen } from "@/components/login-screen";
import { useAuth } from "@/lib/auth";

/**
 * 서비스 준비 중(닫힘) 화면.
 * - 기본: 비로그인 방문자에게 '준비 중' + "운영자 로그인" 노출.
 * - blocked: 로그인은 됐으나 허용 운영자(haru·sky·admin)가 아닌 계정 — 차단 안내 + 로그아웃.
 * PG(카카오페이/카드사) 심사 요건이 갖춰질 때까지 공개엔 닫아둔다.
 */
export function Maintenance({ blocked }: { blocked?: boolean }) {
  const { logout } = useAuth();
  const [showLogin, setShowLogin] = React.useState(false);

  if (showLogin && !blocked) return <LoginScreen />;

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-5 bg-muted/30 px-6 text-center">
      <Logo size={128} className="size-16" />
      {blocked ? (
        <>
          <div className="space-y-1.5">
            <h1 className="text-2xl font-extrabold tracking-tight">
              접근 권한이 없습니다
            </h1>
            <p className="text-sm text-muted-foreground">
              서비스 준비 중이라 지정된 운영자만 이용할 수 있어요.
            </p>
          </div>
          <button
            type="button"
            onClick={() => {
              logout();
              window.location.reload();
            }}
            className="mt-1 rounded-xl border border-border px-4 py-2 text-sm font-semibold transition-colors hover:bg-muted"
          >
            로그아웃
          </button>
        </>
      ) : (
        <>
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
        </>
      )}
    </div>
  );
}
