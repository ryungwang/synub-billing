import Link from "next/link";
import { COMPANY } from "@/lib/company";

/**
 * 공개 페이지 하단 푸터 — 전자상거래법 사업자 표시사항 + 약관/개인정보/환불 링크.
 * PG 심사(사업자정보·약관·개인정보·환불 확인)의 핵심 표시 영역.
 */
export function SiteFooter() {
  const info: Array<[string, string]> = [
    ["상호", COMPANY.legalName],
    ["대표자", COMPANY.ceo],
    ["사업자등록번호", COMPANY.bizRegNo],
    ["통신판매업신고", COMPANY.mailOrderNo],
    ["주소", COMPANY.address],
    ["고객센터", COMPANY.tel],
    ["이메일", COMPANY.email],
  ];

  return (
    <footer className="mt-16 border-t border-border bg-card">
      <div className="mx-auto max-w-5xl px-6 py-10">
        <div className="flex flex-wrap items-center gap-x-5 gap-y-2 text-sm font-semibold">
          <Link href="/pricing" className="hover:text-foreground">요금</Link>
          <span className="text-border">·</span>
          <Link href="/faq" className="hover:text-foreground">자주 묻는 질문</Link>
          <span className="text-border">·</span>
          <Link href="/contact" className="hover:text-foreground">문의</Link>
          <span className="text-border">·</span>
          <Link href="/terms" className="hover:text-foreground">이용약관</Link>
          <span className="text-border">·</span>
          <Link href="/privacy" className="font-bold hover:text-foreground">개인정보처리방침</Link>
          <span className="text-border">·</span>
          <Link href="/refund" className="hover:text-foreground">환불·청약철회</Link>
          <span className="text-border">·</span>
          <a href={COMPANY.homepage} target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
            회사 홈페이지
          </a>
        </div>

        <dl className="mt-5 flex flex-wrap gap-x-5 gap-y-1.5 text-[13px] leading-relaxed text-muted-foreground">
          {info.map(([k, v]) => (
            <div key={k} className="flex gap-1.5">
              <dt className="text-muted-foreground/70">{k}</dt>
              <dd>{v}</dd>
            </div>
          ))}
        </dl>

        <p className="mt-4 text-xs text-muted-foreground/70">
          © {COMPANY.enName} 통신판매중개자가 아닌 통신판매업자로서 자체 SaaS 구독 결제를 제공합니다.
        </p>
      </div>
    </footer>
  );
}
