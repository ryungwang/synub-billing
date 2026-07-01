"use client";

import * as React from "react";
import { cn, formatKRW } from "@/lib/utils";

const CHART_H = 132; // 막대 영역 높이(px) — flex % 높이 이슈 회피로 px 고정

export function MiniBarChart({
  data,
}: {
  data: { month: string; amount: number }[];
}) {
  const max = Math.max(...data.map((d) => d.amount), 1);
  const lastIdx = data.length - 1;

  return (
    <div>
      <div
        className="flex items-end gap-2.5 sm:gap-4"
        style={{ height: CHART_H }}
      >
        {data.map((d, i) => {
          const h = Math.max(8, Math.round((d.amount / max) * CHART_H));
          const active = i === lastIdx;
          return (
            <div
              key={d.month}
              className="group relative flex flex-1 justify-center"
              style={{ height: CHART_H }}
            >
              {/* 툴팁 */}
              <div
                className="pointer-events-none absolute left-1/2 z-10 -translate-x-1/2 whitespace-nowrap rounded-lg bg-popover px-2 py-1 text-[11px] font-bold text-popover-foreground opacity-0 shadow-[var(--shadow-pop)] transition-opacity group-hover:opacity-100 tnum"
                style={{ bottom: h + 8 }}
              >
                {formatKRW(d.amount)}
              </div>
              <div
                className={cn(
                  "absolute bottom-0 w-full rounded-lg transition-all duration-500",
                  active
                    ? "bg-primary"
                    : "bg-primary/20 group-hover:bg-primary/40"
                )}
                style={{ height: h }}
              />
            </div>
          );
        })}
      </div>
      <div className="mt-2.5 flex gap-2.5 sm:gap-4">
        {data.map((d, i) => (
          <span
            key={d.month}
            className={cn(
              "flex-1 text-center text-[11px] font-medium",
              i === lastIdx ? "text-foreground" : "text-muted-foreground"
            )}
          >
            {d.month}
          </span>
        ))}
      </div>
    </div>
  );
}
