import type { Metadata } from "next";
import "./globals.css";
import { AuthGate } from "@/components/auth-gate";

export const metadata: Metadata = {
  title: "Synub Billing — 통합 결제·구독 관리",
  description:
    "신업의 여러 SaaS 제품을 하나의 계정으로 구독하고 결제를 관리하세요.",
  // 결제·구독 관리 앱(로그인 게이트)이라 검색 색인 제외 — 사업자 주소 등이 검색에 노출되지 않도록.
  // PG 심사 스크래퍼는 URL을 직접 읽으므로 영향 없음. 마케팅/SEO는 회사 홈(synub.io)이 담당.
  robots: { index: false, follow: false },
  manifest: "/site.webmanifest",
  icons: {
    icon: [
      {
        url: "/favicon-light-32x32.png",
        media: "(prefers-color-scheme: light)",
        sizes: "32x32",
        type: "image/png",
      },
      {
        url: "/favicon-dark-32x32.png",
        media: "(prefers-color-scheme: dark)",
        sizes: "32x32",
        type: "image/png",
      },
      { url: "/favicon.ico", sizes: "any" },
    ],
    apple: "/apple-touch-icon.png",
  },
};

// 다크모드 깜빡임 방지 — 렌더 전 테마 적용 (시스템 선호 + localStorage)
const themeScript = `
(function() {
  try {
    var t = localStorage.getItem('theme');
    var d = t ? t === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.classList.toggle('dark', d);
  } catch (e) {}
})();
`;

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      </head>
      <body>
        <AuthGate>{children}</AuthGate>
      </body>
    </html>
  );
}
