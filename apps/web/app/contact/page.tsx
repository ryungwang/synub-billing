import type { Metadata } from "next";
import Link from "next/link";
import { Mail, MessageSquare, Clock, MapPin } from "lucide-react";
import { COMPANY } from "@/lib/company";
import { ContactForm, type CatalogItem } from "./contact-form";

// 카탈로그를 서버에서 렌더(공개·CORS 무관)해 폼 제품 선택지를 항상 채운다.
export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "문의하기 — Synub Billing",
  description:
    "Synub Billing 결제·구독 문의. 이메일 또는 온라인 문의 폼으로 접수하거나 자주 묻는 질문에서 빠르게 확인하세요.",
};

async function getCatalog(): Promise<CatalogItem[]> {
  const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  try {
    const res = await fetch(`${base}/products`, { cache: "no-store" });
    if (!res.ok) return [];
    const list = (await res.json()) as Array<{
      serviceCode: string;
      name: string;
      status: string;
      plans: Array<{ code: string; name: string }>;
    }>;
    return list.map((p) => ({
      serviceCode: p.serviceCode,
      name: p.name,
      status: p.status,
      plans: p.plans.map((pl) => ({ code: pl.code, name: pl.name })),
    }));
  } catch {
    return [];
  }
}

export default async function ContactPage() {
  const catalog = await getCatalog();
  return (
    <div className="mx-auto max-w-3xl">
      <header>
        <h1 className="text-2xl font-extrabold tracking-tight">문의하기</h1>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
          결제·구독에 대해 궁금한 점이 있으신가요? <strong className="font-semibold text-foreground">이메일 또는 아래 온라인 문의 폼</strong>으로
          연락 주시면 도와드리겠습니다. 전화 상담은 운영하지 않으며, 접수된 문의는 운영시간 내 순차 답변드립니다.
          자주 묻는 내용은{" "}
          <Link href="/faq" className="font-semibold text-primary hover:underline">
            자주 묻는 질문
          </Link>
          에서 더 빠르게 확인하실 수 있어요.
        </p>
      </header>

      {/* 연락 채널 */}
      <div className="mt-8 grid gap-4 sm:grid-cols-2">
        <a
          href={`mailto:${COMPANY.email}`}
          className="flex items-start gap-3.5 rounded-2xl border border-border bg-card p-5 transition-colors hover:border-primary/40 hover:bg-primary-subtle/40"
        >
          <span className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-primary-subtle text-primary">
            <Mail className="size-5" />
          </span>
          <div>
            <p className="text-sm font-bold">이메일</p>
            <p className="mt-0.5 text-sm text-muted-foreground">{COMPANY.email}</p>
            <p className="mt-1 text-xs text-muted-foreground/70">24시간 접수 · 순차 답변</p>
          </div>
        </a>

        <a
          href="#inquiry"
          className="flex items-start gap-3.5 rounded-2xl border border-border bg-card p-5 transition-colors hover:border-primary/40 hover:bg-primary-subtle/40"
        >
          <span className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-primary-subtle text-primary">
            <MessageSquare className="size-5" />
          </span>
          <div>
            <p className="text-sm font-bold">온라인 문의</p>
            <p className="mt-0.5 text-sm text-muted-foreground">문의 폼으로 바로 접수</p>
            <p className="mt-1 text-xs text-muted-foreground/70">로그인 없이 이용 가능</p>
          </div>
        </a>

        <div className="flex items-start gap-3.5 rounded-2xl border border-border bg-card p-5">
          <span className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-primary-subtle text-primary">
            <Clock className="size-5" />
          </span>
          <div>
            <p className="text-sm font-bold">답변 운영시간</p>
            <p className="mt-0.5 text-sm text-muted-foreground">
              평일 10:00–18:00
            </p>
            <p className="mt-1 text-xs text-muted-foreground/70">주말·공휴일 휴무</p>
          </div>
        </div>

        <div className="flex items-start gap-3.5 rounded-2xl border border-border bg-card p-5">
          <span className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-primary-subtle text-primary">
            <MapPin className="size-5" />
          </span>
          <div>
            <p className="text-sm font-bold">주소</p>
            <p className="mt-0.5 text-sm leading-relaxed text-muted-foreground">
              {COMPANY.address}
            </p>
          </div>
        </div>
      </div>

      {/* 온라인 문의 폼 */}
      <div id="inquiry" className="mt-8 scroll-mt-6">
        <ContactForm catalog={catalog} />
      </div>
    </div>
  );
}
