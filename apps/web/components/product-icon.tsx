import * as React from "react";
import {
  FileText,
  Hash,
  ReceiptText,
  Headset,
  type LucideIcon,
} from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * 제품 아이콘 — 공식 디자인 시스템 규칙(단일 파랑, 액센트색 없음)에 따라
 * 모든 제품은 blue-weak 배경 + blue 아이콘으로 통일하고, 구분은 아이콘 '모양'으로만 한다.
 * (브랜드 3색은 로고에만 — UI로 끌어오지 않음)
 */
export const PRODUCT_ICON: Record<string, LucideIcon> = {
  "문서분석 AI": FileText,
  "포스트플로우": Hash,
  "회계 자동화": ReceiptText,
  "Synub Works": ReceiptText,
  "고객지원 데스크": Headset,
};

const FALLBACK: LucideIcon = FileText;

const SIZES = {
  sm: { box: "size-9 rounded-xl", icon: "size-[18px]" },
  md: { box: "size-11 rounded-2xl", icon: "size-5" },
  lg: { box: "size-12 rounded-2xl", icon: "size-6" },
  xl: { box: "size-14 rounded-2xl", icon: "size-7" },
} as const;

export function ProductIcon({
  name,
  size = "md",
  className,
}: {
  name: string;
  size?: keyof typeof SIZES;
  className?: string;
}) {
  const Icon = PRODUCT_ICON[name] ?? FALLBACK;
  const s = SIZES[size];
  return (
    <span
      className={cn(
        "flex shrink-0 items-center justify-center bg-primary-subtle text-primary",
        s.box,
        className
      )}
    >
      <Icon className={s.icon} strokeWidth={2.2} />
    </span>
  );
}
