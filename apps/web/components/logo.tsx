import * as React from "react";
import Image from "next/image";
import { cn } from "@/lib/utils";

/**
 * Synub 브랜드 심볼 — 공식 에셋(아이소메트릭 큐브 + 헥사곤).
 * 라이트/다크 변형을 테마에 맞춰 노출(no-flash: CSS 토글).
 * 브랜드 3색(라임·코랄·하늘파랑)은 이 심볼 안에서만 — UI 토큰으로 끌어오지 말 것.
 */
export function Logo({
  className,
  size = 32,
}: {
  className?: string;
  size?: number;
}) {
  return (
    <span
      className={cn("relative inline-block", className)}
      style={{ width: size, height: size }}
    >
      <Image
        src="/brand/synub-symbol-light.png"
        alt="Synub"
        width={size}
        height={size}
        priority
        className="block h-full w-full object-contain dark:hidden"
      />
      <Image
        src="/brand/synub-symbol-dark.png"
        alt="Synub"
        width={size}
        height={size}
        priority
        className="hidden h-full w-full object-contain dark:block"
      />
    </span>
  );
}
