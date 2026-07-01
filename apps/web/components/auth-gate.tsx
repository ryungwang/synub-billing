"use client";

import * as React from "react";
import { Loader2 } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { LoginScreen } from "@/components/login-screen";
import { useAuth } from "@/lib/auth";

/** 로그인 하드게이트. 로그인 전에는 앱(사이드바/데이터)을 렌더하지 않고 로그인 화면만 보여준다. */
export function AuthGate({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [mounted, setMounted] = React.useState(false);
  React.useEffect(() => setMounted(true), []);

  // SSR/hydration 불일치 방지 — 마운트 전엔 중립 로딩
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
