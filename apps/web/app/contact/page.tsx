import type { Metadata } from "next";
import Link from "next/link";
import { Mail, Phone, Clock, MapPin } from "lucide-react";
import { COMPANY } from "@/lib/company";
import { ContactForm } from "./contact-form";

export const metadata: Metadata = {
  title: "문의하기 — Synub Billing",
  description:
    "Synub Billing 결제·구독 문의. 이메일·전화로 문의하거나 자주 묻는 질문에서 빠르게 확인하세요.",
};

export default function ContactPage() {
  const tel = COMPANY.tel.replace(/[^0-9+]/g, "");
  return (
    <div className="mx-auto max-w-3xl">
      <header>
        <h1 className="text-2xl font-extrabold tracking-tight">문의하기</h1>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
          결제·구독에 대해 궁금한 점이 있으신가요? 아래 채널로 연락 주시면
          도와드리겠습니다. 자주 묻는 내용은{" "}
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
          href={`tel:${tel}`}
          className="flex items-start gap-3.5 rounded-2xl border border-border bg-card p-5 transition-colors hover:border-primary/40 hover:bg-primary-subtle/40"
        >
          <span className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-primary-subtle text-primary">
            <Phone className="size-5" />
          </span>
          <div>
            <p className="text-sm font-bold">고객센터</p>
            <p className="mt-0.5 text-sm text-muted-foreground">{COMPANY.tel}</p>
            <p className="mt-1 text-xs text-muted-foreground/70">평일 10:00–18:00</p>
          </div>
        </a>

        <div className="flex items-start gap-3.5 rounded-2xl border border-border bg-card p-5">
          <span className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-primary-subtle text-primary">
            <Clock className="size-5" />
          </span>
          <div>
            <p className="text-sm font-bold">운영 시간</p>
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

      {/* 문의 폼 */}
      <div className="mt-8">
        <ContactForm />
      </div>
    </div>
  );
}
