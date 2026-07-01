import * as React from "react";
import { cn } from "@/lib/utils";
import type { Usage } from "@/lib/mock-data";

export function UsageBar({
  usage,
  className,
}: {
  usage: Usage;
  className?: string;
}) {
  const unlimited = usage.limit === null;
  const pct = unlimited
    ? 0
    : Math.min(100, Math.round((usage.used / usage.limit!) * 100));

  const tone =
    pct >= 100
      ? "bg-destructive"
      : pct >= 85
        ? "bg-warning"
        : "bg-primary";

  return (
    <div className={className}>
      <div className="flex items-baseline justify-between text-[13px]">
        <span className="font-medium text-muted-foreground">
          {usage.label}
        </span>
        <span className="font-semibold tnum">
          {usage.used.toLocaleString()}
          <span className="text-muted-foreground">
            {unlimited
              ? ` / 무제한`
              : ` / ${usage.limit!.toLocaleString()}${usage.unit}`}
          </span>
        </span>
      </div>
      <div className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-muted">
        <div
          className={cn(
            "h-full rounded-full transition-[width] duration-500",
            unlimited ? "bg-primary/40" : tone
          )}
          style={{ width: unlimited ? "18%" : `${Math.max(pct, 3)}%` }}
        />
      </div>
    </div>
  );
}
