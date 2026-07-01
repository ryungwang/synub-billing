"use client";

import * as React from "react";
import { Loader2 } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { LoginScreen } from "@/components/login-screen";
import { useAuth } from "@/lib/auth";
import { getToken, getRefreshToken, refreshAccessToken } from "@/lib/token";

/** 로그인 하드게이트. 로그인 전에는 앱(사이드바/데이터)을 렌더하지 않고 로그인 화면만 보여준다. */
export function AuthGate({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    // 액세스 토큰이 만료됐지만 리프레시가 있으면 조용히 갱신 후 진입(로그인 화면 깜빡임 방지)
    (async () => {
      if (!getToken() && getRefreshToken()) {
        await refreshAccessToken();
      }
      setMounted(true);
    })();
  }, []);

  // SSR/hydration 불일치 방지 + 초기 갱신 대기 — 마운트 전엔 중립 로딩
  if (!mounted) {
    return (
      <div className="flex min-h-dvh items-center justify-center text-muted-foreground">
        <Loader2 className="size-6 animate-spin" />
      </div>
    );
  }

  if (!user) return <LoginScreen />;
  return <AppShell>{children}</AppShell>;
}
