"use client";

import * as React from "react";
import { usePathname } from "next/navigation";
import { Loader2 } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { PublicShell } from "@/components/public-shell";
import { LoginScreen } from "@/components/login-screen";
import { Maintenance } from "@/components/maintenance";
import { useAuth } from "@/lib/auth";
import {
  getToken,
  refreshAccessToken,
  rawToken,
  subscribeToken,
  decode,
} from "@/lib/token";

// 로그인 없이 공개되는 경로 — 요금/약관/개인정보/환불(전자상거래 표시·PG 심사).
// 서버 렌더 단계에서 바로 통과시켜 스크래퍼·검색엔진이 콘텐츠를 볼 수 있게 한다.
// "/product"는 공개 제품 상세(/product/{code})만 매칭 — 인증 카탈로그 "/products"(복수)와는 별개(접두 불일치).
const PUBLIC_PREFIXES = [
  "/pricing",
  "/product",
  "/faq",
  "/contact",
  "/terms",
  "/privacy",
  "/refund",
];

// 홈페이지 닫힘 플래그 — PG(카카오페이/카드사) 심사 요건(유선전화·판매상품 등) 갖춰질 때까지 true.
// true면 비로그인 방문자에게 '서비스 준비 중'만 노출(운영자 로그인은 유지). 열려면 false.
const SITE_CLOSED = true;

// 닫힘 기간 중 로그인 허용 계정(external_id) — haru·sky·admin 세 개발사 계정만.
// 그 외 계정은 로그인해도 차단(Maintenance blocked). 열면(SITE_CLOSED=false) 전체 허용.
const ALLOWED_SUBS = ["usr_admin_haru", "usr_office_sky", "usr_office_admin"];

// 닫힘 기간 테스트용 지정 일반 계정 — 로그인 아이디(=토큰 email)로 허용. deer 계정 하나.
const ALLOWED_EMAILS = ["deer"];

/** 로그인 하드게이트. 로그인 전에는 앱(사이드바/데이터)을 렌더하지 않고 로그인 화면만 보여준다. */
export function AuthGate({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { user } = useAuth();
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    // 액세스 토큰이 없으면 통합세션 쿠키(.synub.io)로 조용히 갱신 시도 후 진입.
    // 다른 서비스(office 등)에서 로그인했으면 여기서 폼 없이 세션을 이어받는다(무폼 SSO).
    (async () => {
      if (!getToken()) {
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
      refreshAccessToken().catch(() => {});
    }, Math.max(1000, ms));
    return () => clearTimeout(timer);
  }, [rawTok]);

  // 백그라운드 탭은 타이머가 지연될 수 있어, 탭이 다시 보일 때 만료됐으면 즉시 갱신.
  React.useEffect(() => {
    function onVisible() {
      if (document.visibilityState === "visible" && !getToken()) {
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

  const loader = (
    <div className="flex min-h-dvh items-center justify-center text-muted-foreground">
      <Loader2 className="size-6 animate-spin" />
    </div>
  );

  // 닫힘(PG 심사 요건 갖출 때까지): 비로그인 방문자는 '준비 중', 로그인한 운영자는 정상 사용.
  if (SITE_CLOSED) {
    // 로그인한 운영자는 공개 페이지(요금·문의·약관 등)도 앱 셸 안에서 — 사이드바 네비 유지
    // (공개 셸로 튕기면 '로그인' 버튼이 보여 로그아웃된 듯한 이질감이 생김).
    if (mounted && user) {
      const allowed =
        ALLOWED_SUBS.includes(user.sub) || ALLOWED_EMAILS.includes(user.email);
      if (allowed) return <AppShell>{children}</AppShell>;
      // 로그인했지만 미허용 계정: 공개 페이지는 열람 허용, 앱은 차단.
      if (isPublic) return <PublicShell>{children}</PublicShell>;
      return <Maintenance blocked />;
    }
    // 요금·약관·개인정보·환불·문의는 닫힘 중에도 항상 공개 — 전자상거래 표시·PG 심사자가 로그인 없이 봐야 함.
    // (SSR/비로그인 단계: 공개 콘텐츠를 그대로 노출해 스크래퍼·검색엔진이 볼 수 있게 한다.)
    if (isPublic) return <PublicShell>{children}</PublicShell>;
    if (!mounted) return loader;
    return <Maintenance />;
  }

  // 오픈 상태: 로그인한 사용자는 공개 페이지도 앱 셸 안에서 본다.
  if (mounted && user) return <AppShell>{children}</AppShell>;

  if (isPublic) return <PublicShell>{children}</PublicShell>;

  // SSR/hydration 불일치 방지 + 초기 갱신 대기 — 마운트 전엔 중립 로딩
  if (!mounted) return loader;

  return <LoginScreen />;
}
