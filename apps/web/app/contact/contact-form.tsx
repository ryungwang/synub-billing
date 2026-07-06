"use client";

import * as React from "react";
import { Send, CheckCircle2, Paperclip, X, Loader2, AlertCircle } from "lucide-react";
import { COMPANY } from "@/lib/company";
import { api } from "@/lib/api";
import { cn } from "@/lib/utils";

const TYPES = ["결제·구독", "플랜 변경·해지", "기술 지원", "제휴·기타"] as const;

const MAX_ATTACHMENT = 10 * 1024 * 1024; // 10MB — 서버 상한과 정합
const ACCEPT = ["image/png", "image/jpeg", "application/pdf"];
const ACCEPT_ATTR = "image/png,image/jpeg,application/pdf,.png,.jpg,.jpeg,.pdf";

/** 서버(문의 페이지)에서 넘겨받는 제품 카탈로그 항목. */
export type CatalogItem = {
  serviceCode: string;
  name: string;
  status: string;
  plans: { code: string; name: string }[];
};

/** 드롭다운 옵션 — code(제품코드)·short(제목용 짧은 이름)·label(표시용 전체). */
type ProductOption = { value: string; code: string; short: string; label: string };

function catalogOptions(catalog: CatalogItem[]): ProductOption[] {
  return catalog
    .filter((p) => p.status === "active" || p.status === "coming_soon")
    .map((p) => ({ value: p.serviceCode, code: p.serviceCode, short: p.name, label: p.name }));
}

function humanSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

/**
 * 문의 폼 — /inquiries API로 접수(선택적 파일 첨부). 접수는 서버에 저장되고 고객센터로 알림 발송.
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
  const [website, setWebsite] = React.useState(""); // 허니팟(봇 차단)
  const [file, setFile] = React.useState<File | null>(null);
  const [fileErr, setFileErr] = React.useState<string | null>(null);
  const [dragging, setDragging] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [done, setDone] = React.useState(false);
  const fileInputRef = React.useRef<HTMLInputElement>(null);

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
          code: s.serviceCode,
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

  function pickFile(f: File | null) {
    setError(null);
    if (!f) {
      setFile(null);
      setFileErr(null);
      return;
    }
    const okType =
      ACCEPT.includes(f.type) || /\.(png|jpe?g|pdf)$/i.test(f.name);
    if (!okType) {
      setFile(null);
      setFileErr("이미지(JPG/PNG) 또는 PDF 파일만 첨부할 수 있습니다.");
      return;
    }
    if (f.size > MAX_ATTACHMENT) {
      setFile(null);
      setFileErr("첨부파일은 10MB 이하만 업로드할 수 있습니다.");
      return;
    }
    setFileErr(null);
    setFile(f);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (fileErr || submitting) return;
    setError(null);
    setSubmitting(true);
    try {
      const sel = options.find((o) => o.value === product);
      const fd = new FormData();
      fd.append("type", type);
      if (sel) {
        fd.append("productCode", sel.code);
        fd.append("productLabel", sel.label);
      }
      fd.append("name", name);
      fd.append("email", email);
      fd.append("message", message);
      fd.append("website", website); // 허니팟 — 봇이 채우면 서버가 폐기
      if (file) fd.append("attachment", file);
      await api.submitInquiry(fd);
      setDone(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "문의 접수에 실패했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setSubmitting(false);
    }
  }

  if (done) {
    return (
      <div className="rounded-2xl border border-border bg-card p-8 text-center sm:p-10">
        <span className="mx-auto flex size-12 items-center justify-center rounded-2xl bg-success-subtle text-success-foreground">
          <CheckCircle2 className="size-6" />
        </span>
        <h2 className="mt-4 text-lg font-extrabold tracking-tight">문의가 접수되었습니다</h2>
        <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">
          {email ? <><strong className="font-semibold text-foreground">{email}</strong> 로 </> : ""}
          운영시간 내 순차적으로 답변드리겠습니다. 감사합니다.
        </p>
        <button
          type="button"
          onClick={() => {
            setDone(false);
            setMessage("");
            setFile(null);
            setProduct("");
          }}
          className="mt-5 inline-flex items-center justify-center rounded-full border border-border px-4 py-2 text-sm font-semibold text-muted-foreground transition-colors hover:text-foreground"
        >
          문의 다시 남기기
        </button>
      </div>
    );
  }

  return (
    <form
      onSubmit={submit}
      className="rounded-2xl border border-border bg-card p-6 sm:p-7"
    >
      <h2 className="text-lg font-extrabold tracking-tight">문의 남기기</h2>
      <p className="mt-1 text-[13px] text-muted-foreground">
        아래 내용을 작성해 보내주시면 접수되고, 회신 이메일로 답변드립니다.
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

        {/* 첨부파일 (선택) */}
        <div>
          <span className="mb-1.5 block text-[13px] font-semibold text-foreground">
            첨부파일 <span className="font-normal text-muted-foreground">(선택 · 이미지·PDF · 최대 10MB)</span>
          </span>
          <input
            ref={fileInputRef}
            type="file"
            accept={ACCEPT_ATTR}
            className="hidden"
            onChange={(e) => {
              pickFile(e.target.files?.[0] ?? null);
              e.target.value = "";
            }}
          />
          {file ? (
            <div className="flex items-center gap-3 rounded-xl border border-border bg-background px-3.5 py-3">
              <Paperclip className="size-4 shrink-0 text-muted-foreground" />
              <span className="min-w-0 flex-1 truncate text-sm">{file.name}</span>
              <span className="shrink-0 text-xs text-muted-foreground">{humanSize(file.size)}</span>
              <button
                type="button"
                onClick={() => pickFile(null)}
                className="shrink-0 rounded-full p-1 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                aria-label="첨부 제거"
              >
                <X className="size-4" />
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              onDragOver={(e) => {
                e.preventDefault();
                setDragging(true);
              }}
              onDragLeave={() => setDragging(false)}
              onDrop={(e) => {
                e.preventDefault();
                setDragging(false);
                pickFile(e.dataTransfer.files?.[0] ?? null);
              }}
              className={cn(
                "flex w-full items-center justify-center gap-2 rounded-xl border border-dashed px-3.5 py-5 text-sm transition-colors",
                dragging
                  ? "border-primary bg-primary-subtle/40 text-primary"
                  : "border-border text-muted-foreground hover:border-primary/40 hover:text-foreground"
              )}
            >
              <Paperclip className="size-4" />
              파일 선택 또는 여기로 드래그
            </button>
          )}
          {fileErr && (
            <p className="mt-1.5 flex items-center gap-1.5 text-[13px] font-medium text-destructive">
              <AlertCircle className="size-3.5" />
              {fileErr}
            </p>
          )}
        </div>

        {/* 허니팟 — 사람 눈엔 안 보이고 봇만 채움 */}
        <input
          type="text"
          name="website"
          value={website}
          onChange={(e) => setWebsite(e.target.value)}
          tabIndex={-1}
          autoComplete="off"
          aria-hidden="true"
          className="absolute left-[-9999px] h-0 w-0 opacity-0"
        />
      </div>

      {error && (
        <p className="mt-4 flex items-center gap-2 rounded-xl bg-destructive-subtle px-3.5 py-2.5 text-sm font-medium text-destructive-subtle-foreground">
          <AlertCircle className="size-4 shrink-0" />
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={submitting || !!fileErr}
        className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-full bg-primary px-5 py-3 text-sm font-bold text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-50"
      >
        {submitting ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
        {submitting ? "접수 중…" : "문의 접수하기"}
      </button>

      <p className="mt-3 text-center text-xs text-muted-foreground/70">
        접수가 어려우면 {COMPANY.email} 로 직접 보내주셔도 됩니다.
      </p>
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
