import { ImageResponse } from "next/og";
import { readFileSync } from "fs";
import { join } from "path";

// 링크 미리보기(OG) 카드 — 브랜드 심볼 + Synub 워드마크. 다크 프리미엄, 폰트 의존 없음(라틴 기본 폰트).
export const runtime = "nodejs";
export const alt = "Synub — 통합계정 하나로 모든 SaaS 구독·결제";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default function OpengraphImage() {
  const symbol = `data:image/png;base64,${readFileSync(
    join(process.cwd(), "public/brand/synub-symbol-dark.png")
  ).toString("base64")}`;

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          background:
            "radial-gradient(130% 130% at 50% 15%, #14233f 0%, #0a0f1c 62%)",
          color: "#ffffff",
        }}
      >
        <div
          style={{ display: "flex", alignItems: "center", gap: 28, marginBottom: 26 }}
        >
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={symbol} width={116} height={116} alt="" />
          <span style={{ fontSize: 104, fontWeight: 800, letterSpacing: -3 }}>
            Synub
          </span>
        </div>
        <span style={{ fontSize: 34, fontWeight: 500, color: "#93a4bd" }}>
          One account for every SaaS · subscriptions & billing
        </span>
        <span
          style={{
            marginTop: 46,
            fontSize: 22,
            fontWeight: 600,
            letterSpacing: 4,
            color: "#5b6b86",
          }}
        >
          APP.SYNUB.IO
        </span>
      </div>
    ),
    { ...size }
  );
}
