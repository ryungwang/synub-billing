"use client";

import * as React from "react";
import { Send, CheckCircle2 } from "lucide-react";
import { COMPANY } from "@/lib/company";
import { api } from "@/lib/api";
import { cn } from "@/lib/utils";

const TYPES = ["결제·구독", "플랜 변경·해지", "기술 지원", "제휴·기타"] as const;

/** 서버(문의 페이지)에서 넘겨받는 제품 카탈로그 항목. */
export type CatalogItem = {
  serviceCode: string;
  name: string;
  status: string;
  plans: { code: string; name: string }[];
};

/** 드롭다운 옵션 — short(제목용 짧은 이름)·label(표시용 전체). */
type ProductOption = { value: string; short: string; label: string };

function catalogOptions(catalog: CatalogItem[]): ProductOption[] {
  return catalog
    .filter((p) => p.status === "active" || p.status === "coming_soon")
    .map((p) => ({ value: p.serviceCode, short: p.name, label: p.name }));
}

/**
 * 문의 폼 — 백엔드 없이 사용자의 메일 앱으로 작성 내용을 전달(mailto compose).
 * 별도 접수 서버가 없으므로 정직하게 "메일 앱이 열립니다"로 안내한다.
 * 제품 선택지: 카탈로그는 서버에서 prop으로(공개·CORS 무관), 로그인 시 내 구독으로 업그레이드.
 */
export function ContactForm({ catalog }: { catalog: CatalogItem[] }) {
  const [type, setType] = React.useState<string>(TYPES[0]);
  const [product, setProduct] = React.useState("");
  // 기본값: 서버가 넘긴 카탈로그(항상 채워짐). 로그인 구독자면 useEffect에서 구독 목록으로 교체.
  const [options, setOptions] = React.useState<ProductOption[]>(() =>
    catalogOptions(catalog)
  );
  // "sub": 로그인 구독자 → '구독 제품' / "catalog": 비로그인·미구독 → '관련 제품'
  const [mode, setMode] = React.useState<"sub" | "catalog">("catalog");
  const [name, setName] = React.useState("");
  const [email, setEmail] = React.useState("");
  const [message, setMessage] = React.useState("");
  const [opened, setOpened] = React.useState(false);

  React.useEffect(() => {
    let cancelled = false;
    const nameOf = (code: string) =>
      catalog.find((p) => p.serviceCode === code)?.name ?? code;
    const planNameOf = (code: string, planCode: string) =>
      catalog
        .find((p) => p.serviceCode === code)
        ?.plans.find((pl) => pl.code === planCode)?.name ?? planCode;

    (async () => {
      // 로그인 상태면 내 구독으로 업그레이드(비로그인/미구독/CORS 실패면 카탈로그 유지).
      let subs: Awaited<ReturnType<typeof api.mySubscriptions>> = [];
      try {
        subs = await api.mySubscriptions();
      } catch {
        subs = [];
      }
      if (cancelled || subs.length === 0) return;

      const opts = subs.map((s) => {
        const pname = nameOf(s.serviceCode);
        const plan = planNameOf(s.serviceCode, s.plan);
        const scope = s.scope === "org" && s.orgName ? ` · ${s.orgName}` : "";
        return {
          value: `${s.serviceCode}#${s.plan}#${s.scope}${s.orgName ?? ""}`,
          short: pname,
          label: `${pname} · ${plan}${scope}`,
        };
      });
      setOptions(opts);
      setMode("sub");
    })();
    return () => {
      cancelled = true;
    };
  }, [catalog]);

  function submit(e: React.FormEvent) {
    e.preventDefault();
    const sel = options.find((o) => o.value === product);
    const subject = `[문의:${type}${sel ? ` · ${sel.short}` : ""}] ${
      name || "고객"
    }님의 문의`;
    const body =
      `문의 유형: ${type}\n` +
      (sel ? `제품: ${sel.label}\n` : "") +
      `이름: ${name}\n` +
      `회신 이메일: ${email}\n` +
      `\n${message}\n`;
    window.location.href = `mailto:${COMPANY.email}?subject=${encodeURIComponent(
      subject
    )}&body=${encodeURIComponent(body)}`;
    setOpened(true);
  }

  return (
    <form
      onSubmit={submit}
      className="rounded-2xl border border-border bg-card p-6 sm:p-7"
    >
      <h2 className="text-lg font-extrabold tracking-tight">문의 남기기</h2>
      <p className="mt-1 text-[13px] text-muted-foreground">
        아래 내용을 작성하고 보내면 메일 앱이 열립니다. 확인 후 전송해 주세요.
      </p>

      <div className="mt-5 space-y-4">
        <div>
          <span className="mb-1.5 block text-[13px] font-semibold text-foreground">
            문의 유형
          </span>
          <div className="flex flex-wrap gap-2">
            {TYPES.map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setType(t)}
                className={cn(
                  "rounded-full border px-3.5 py-1.5 text-sm font-semibold transition-colors",
                  type === t
                    ? "border-primary bg-primary text-primary-foreground"
                    : "border-border text-muted-foreground hover:text-foreground"
                )}
              >
                {t}
              </button>
            ))}
          </div>
        </div>

        {options.length > 0 && (
          <label className="block">
            <span className="mb-1.5 block text-[13px] font-semibold text-foreground">
              {mode === "sub" ? "구독 제품" : "관련 제품"}
              <span className="ml-1 font-normal text-muted-foreground">(선택)</span>
            </span>
            <select
              value={product}
              onChange={(e) => setProduct(e.target.value)}
              className="w-full appearance-none rounded-xl border border-border bg-background bg-[length:1.1rem] bg-[right_0.85rem_center] bg-no-repeat px-3.5 py-3 pr-10 text-sm outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/20"
              style={{
                backgroundImage:
                  "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' fill='none' stroke='%2394a3b8' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' viewBox='0 0 24 24'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E\")",
              }}
            >
              <option value="">선택 안 함 / 일반 문의</option>
              {options.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </label>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="이름" value={name} onChange={setName} placeholder="홍길동" />
          <Field
            label="회신 이메일"
            value={email}
            onChange={setEmail}
            type="email"
            placeholder="you@example.com"
            required
          />
        </div>

        <label className="block">
          <span className="mb-1.5 block text-[13px] font-semibold text-foreground">
            문의 내용
          </span>
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            required
            rows={5}
            placeholder="문의하실 내용을 자세히 적어주세요."
            className="w-full resize-y rounded-xl border border-border bg-background px-3.5 py-3 text-sm outline-none transition-colors placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20"
          />
        </label>
      </div>

      <button
        type="submit"
        className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-full bg-primary px-5 py-3 text-sm font-bold text-primary-foreground transition-opacity hover:opacity-90"
      >
        <Send className="size-4" />
        메일 앱으로 문의 보내기
      </button>

      {opened && (
        <p className="mt-3 flex items-center justify-center gap-1.5 text-[13px] font-medium text-success-foreground">
          <CheckCircle2 className="size-4" />
          메일 앱을 열었어요. 열리지 않으면 {COMPANY.email} 로 보내주세요.
        </p>
      )}
    </form>
  );
}

function Field({
  label,
  value,
  onChange,
  type = "text",
  placeholder,
  required,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
  placeholder?: string;
  required?: boolean;
}) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-[13px] font-semibold text-foreground">
        {label}
      </span>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        required={required}
        className="w-full rounded-xl border border-border bg-background px-3.5 py-3 text-sm outline-none transition-colors placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20"
      />
    </label>
  );
}
