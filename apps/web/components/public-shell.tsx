import Link from "next/link";
import { Logo } from "@/components/logo";
import { SiteFooter } from "@/components/site-footer";

/**
 * 로그인 없이 공개되는 페이지(요금·약관·개인정보·환불)의 껍데기.
 * 상단 바(로고+로그인) + 본문 + 사업자정보 푸터. 앱 본체(AppShell)와 분리.
 */
export function PublicShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-dvh flex-col bg-background">
      <header className="border-b border-border bg-card/50 backdrop-blur">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-3.5">
          <Link href="/" className="flex items-center gap-2.5">
            <Logo size={64} className="size-7" />
            <span className="text-base font-extrabold tracking-tight">Synub Billing</span>
          </Link>
          <div className="flex items-center gap-4">
            <Link href="/pricing" className="text-sm font-semibold text-muted-foreground hover:text-foreground">
              요금
            </Link>
            <Link href="/faq" className="hidden text-sm font-semibold text-muted-foreground hover:text-foreground sm:block">
              자주 묻는 질문
            </Link>
            <Link href="/contact" className="text-sm font-semibold text-muted-foreground hover:text-foreground">
              문의
            </Link>
            <Link
              href="/"
              className="rounded-full bg-primary px-4 py-2 text-sm font-bold text-primary-foreground hover:opacity-90"
            >
              로그인
            </Link>
          </div>
        </div>
      </header>
      <main className="flex-1">
        <div className="mx-auto max-w-5xl px-6 py-10">{children}</div>
      </main>
      <SiteFooter />
    </div>
  );
}
