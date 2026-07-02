"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  Sparkles,
  Repeat,
  CreditCard,
  ReceiptText,
  Users,
  ShieldCheck,
  Package,
  Menu,
  X,
  LifeBuoy,
} from "lucide-react";
import { useAuth } from "@/lib/auth";
import { rawContext, subscribeContext, contextOrgId } from "@/lib/context";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme-toggle";
import { Logo } from "@/components/logo";
import { AuthMenu } from "@/components/auth-menu";
import { ContextSwitcher } from "@/components/context-switcher";

type NavItem = { href: string; label: string; icon: typeof LayoutDashboard };
type NavGroup = { title: string; items: NavItem[] };

const NAV: NavGroup[] = [
  {
    title: "서비스",
    items: [
      { href: "/", label: "대시보드", icon: LayoutDashboard },
      { href: "/products", label: "제품 둘러보기", icon: Sparkles },
    ],
  },
  {
    title: "구독·결제",
    items: [
      { href: "/subscriptions", label: "구독 관리", icon: Repeat },
      { href: "/payment-methods", label: "결제수단", icon: CreditCard },
      { href: "/payments", label: "결제 내역", icon: ReceiptText },
    ],
  },
];

// 회사(조직) 컨텍스트에서만 노출
const ORG_GROUP: NavGroup = {
  title: "조직",
  items: [{ href: "/team", label: "멤버", icon: Users }],
};

function NavLink({
  item,
  active,
  onNavigate,
}: {
  item: NavItem;
  active: boolean;
  onNavigate?: () => void;
}) {
  const Icon = item.icon;
  return (
    <Link
      href={item.href}
      onClick={onNavigate}
      className={cn(
        "group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
        active
          ? "bg-sidebar-active text-sidebar-active-foreground"
          : "text-sidebar-foreground hover:bg-muted hover:text-foreground"
      )}
    >
      <Icon
        className={cn(
          "size-[18px] transition-colors",
          active ? "text-sidebar-active-foreground" : "text-sidebar-muted group-hover:text-foreground"
        )}
      />
      {item.label}
    </Link>
  );
}

const ADMIN_GROUP: NavGroup = {
  title: "운영",
  items: [
    { href: "/admin", label: "대시보드", icon: LayoutDashboard },
    { href: "/admin/subscriptions", label: "구독", icon: Repeat },
    { href: "/admin/payments", label: "결제", icon: ReceiptText },
    { href: "/admin/customers", label: "개인 고객", icon: Users },
    { href: "/admin/organizations", label: "회사", icon: ShieldCheck },
    { href: "/admin/products", label: "제품", icon: Package },
  ],
};

function SidebarContent({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname();
  const { user } = useAuth();
  const ctx = React.useSyncExternalStore(subscribeContext, rawContext, () => "personal");
  const isOrg = contextOrgId(ctx) !== null;

  const groups = [...NAV];
  if (isOrg) groups.push(ORG_GROUP); // 회사 컨텍스트에서만 멤버 관리 노출
  if (user?.admin) groups.push(ADMIN_GROUP);

  const isActive = (href: string) =>
    href === "/" || href === "/admin" ? pathname === href : pathname.startsWith(href);

  return (
    <div className="flex h-full flex-col">
      <div className="flex h-16 items-center gap-2.5 px-6">
        <Logo className="size-8" />
        <div className="leading-tight">
          <div className="text-[15px] font-extrabold tracking-tight">
            Synub
          </div>
          <div className="text-[11px] font-medium text-sidebar-muted">
            Billing
          </div>
        </div>
      </div>

      <nav className="flex-1 space-y-6 overflow-y-auto px-3 py-4">
        {groups.map((group) => (
          <div key={group.title}>
            <div className="px-3 pb-1.5 text-[11px] font-bold uppercase tracking-wider text-sidebar-muted">
              {group.title}
            </div>
            <div className="space-y-0.5">
              {group.items.map((item) => (
                <NavLink
                  key={item.href}
                  item={item}
                  active={isActive(item.href)}
                  onNavigate={onNavigate}
                />
              ))}
            </div>
          </div>
        ))}
      </nav>

      <div className="p-3">
        <div className="rounded-2xl bg-muted/60 p-4">
          <div className="flex items-center gap-2 text-sm font-bold">
            <LifeBuoy className="size-4 text-primary" />
            도움이 필요하신가요?
          </div>
          <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
            결제·구독 관련 문의는 고객센터에서 빠르게 도와드려요.
          </p>
          <Button
            variant="outline"
            size="sm"
            className="mt-3 w-full"
            onClick={onNavigate}
          >
            고객센터 문의
          </Button>
        </div>
      </div>
    </div>
  );
}

export function AppShell({ children }: { children: React.ReactNode }) {
  const [mobileOpen, setMobileOpen] = React.useState(false);

  return (
    <div className="min-h-dvh">
      {/* 데스크톱 사이드바 */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-sidebar-border bg-sidebar lg:block">
        <SidebarContent />
      </aside>

      {/* 모바일 드로어 */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div
            className="absolute inset-0 bg-foreground/40 backdrop-blur-[2px] animate-in fade-in-0"
            onClick={() => setMobileOpen(false)}
          />
          <div className="absolute inset-y-0 left-0 w-72 border-r border-sidebar-border bg-sidebar animate-in slide-in-from-left-2">
            <button
              className="absolute right-3 top-4 rounded-lg p-1.5 text-muted-foreground hover:bg-muted"
              onClick={() => setMobileOpen(false)}
              aria-label="메뉴 닫기"
            >
              <X className="size-5" />
            </button>
            <SidebarContent onNavigate={() => setMobileOpen(false)} />
          </div>
        </div>
      )}

      {/* 본문 */}
      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-border bg-background/80 px-4 backdrop-blur-md sm:px-6 lg:px-8">
          <Button
            variant="ghost"
            size="icon-sm"
            className="lg:hidden"
            onClick={() => setMobileOpen(true)}
            aria-label="메뉴 열기"
          >
            <Menu className="size-5" />
          </Button>

          <ContextSwitcher />

          <div className="flex-1" />

          <ThemeToggle />
          <AuthMenu />
        </header>

        <main className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
          {children}
        </main>
      </div>
    </div>
  );
}
