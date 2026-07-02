"use client";

import * as React from "react";
import { usePathname } from "next/navigation";
import { Loader2 } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { PublicShell } from "@/components/public-shell";
import { LoginScreen } from "@/components/login-screen";
import { useAuth } from "@/lib/auth";
import { getToken, getRefreshToken, refreshAccessToken } from "@/lib/token";

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
