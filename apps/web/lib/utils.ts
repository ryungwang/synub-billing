import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/** 원화 포맷 — 1,000 단위 콤마, 통화 기호 옵션 */
export function formatKRW(amount: number, opts?: { withSymbol?: boolean }) {
  const n = new Intl.NumberFormat("ko-KR").format(amount);
  return opts?.withSymbol === false ? n : `₩${n}`;
}

/** YYYY.MM.DD */
export function formatDate(iso: string) {
  const d = new Date(iso);
  const p = (x: number) => String(x).padStart(2, "0");
  return `${d.getFullYear()}.${p(d.getMonth() + 1)}.${p(d.getDate())}`;
}

/** YYYY.MM.DD HH:mm */
export function formatDateTime(iso: string) {
  const d = new Date(iso);
  const p = (x: number) => String(x).padStart(2, "0");
  return `${formatDate(iso)} ${p(d.getHours())}:${p(d.getMinutes())}`;
}
