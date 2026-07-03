"use client";

import * as React from "react";
import { usePathname } from "next/navigation";
import { Loader2 } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { PublicShell } from "@/components/public-shell";
import { LoginScreen } from "@/components/login-screen";
import { useAuth } from "@/lib/auth";
import {
  getToken,
  getRefreshToken,
  refreshAccessToken,
  rawToken,
  subscribeToken,
  decode,
} from "@/lib/token";

// 로그인 없이 공개되는 경로 — 요금/약관/개인정보/환불(전자상거래 표시·PG 심사).
// 서버 렌더 단계에서 바로 통과시켜 스크래퍼·검색엔진이 콘텐츠를 볼 수 있게 한다.
const PUBLIC_PREFIXES = ["/pricing", "/terms", "/privacy", "/refund"];

/** 로그인 하드게이트. 로그인 전에는 앱(사이드바/데이터)을 렌더하지 않고 로그인 화면만 보여준다. */
export function AuthGate({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
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

  // 선제 토큰 갱신 — 액세스 토큰(TTL 60분)이 만료되기 1분 전에 자동 리프레시.
  // 이게 없으면 60분 idle 후 리렌더(예: 우상단 클릭) 시 만료 토큰 → 로그인 버튼으로 바뀌는 문제.
  const rawTok = React.useSyncExternalStore(subscribeToken, rawToken, () => null);
  React.useEffect(() => {
    if (!rawTok) return;
    const u = decode(rawTok);
    if (!u) return;
    const ms = u.exp * 1000 - Date.now() - 60_000; // 만료 1분 전
    const timer = setTimeout(() => {
      if (getRefreshToken()) refreshAccessToken().catch(() => {});
    }, Math.max(1000, ms));
    return () => clearTimeout(timer);
  }, [rawTok]);

  // 백그라운드 탭은 타이머가 지연될 수 있어, 탭이 다시 보일 때 만료됐으면 즉시 갱신.
  React.useEffect(() => {
    function onVisible() {
      if (
        document.visibilityState === "visible" &&
        !getToken() &&
        getRefreshToken()
      ) {
        refreshAccessToken().catch(() => {});
      }
    }
    document.addEventListener("visibilitychange", onVisible);
    return () => document.removeEventListener("visibilitychange", onVisible);
  }, []);

  // 공개 경로는 인증·마운트 대기 없이 그대로 노출(SSR HTML에 콘텐츠 포함) — 훅 선언 뒤에서 분기.
  const isPublic = PUBLIC_PREFIXES.some(
    (p) => pathname === p || pathname.startsWith(p + "/")
  );
  if (isPublic) return <PublicShell>{children}</PublicShell>;

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
