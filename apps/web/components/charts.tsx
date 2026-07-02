"use client";

import * as React from "react";
import { formatKRW } from "@/lib/utils";

/* ── 공통 ─────────────────────────────────────────────
 * dataviz 규칙: 추세/막대는 단일 hue(primary), 상태 분포는 예약 상태색 + 항상 라벨/범례.
 * 마크는 얇게·데이터끝 4px 라운드, 축/그리드는 recessive, 호버 툴팁 기본 제공.
 */

export type Pt = { label: string; value: number };
export type Slice = { name: string; value: number; color: string };

const AXIS = "var(--muted-foreground)";
const GRID = "var(--border)";

/** 월별 매출 — 면적+선(단일 hue). 호버 크로스헤어+툴팁. 컨테이너 실폭을 측정해 1:1로 그려 왜곡 없음. */
export function AreaTrend({ data, height = 200 }: { data: Pt[]; height?: number }) {
  const ref = React.useRef<HTMLDivElement>(null);
  const [W, setW] = React.useState(720);
  const [hi, setHi] = React.useState<number | null>(null);
  React.useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const ro = new ResizeObserver((entries) => {
      for (const e of entries) setW(Math.max(320, Math.round(e.contentRect.width)));
    });
    ro.observe(el);
    return () => ro.disconnect();
  }, []);
  const H = height;
  const padL = 8, padR = 8, padT = 16, padB = 26;
  const iw = W - padL - padR, ih = H - padT - padB;
  const max = Math.max(...data.map((d) => d.value), 1);
  const n = data.length;
  const x = (i: number) => padL + (n <= 1 ? iw / 2 : (i / (n - 1)) * iw);
  const y = (v: number) => padT + ih - (v / max) * ih;
  const pts = data.map((d, i) => [x(i), y(d.value)] as const);
  const line = pts.map((p, i) => `${i ? "L" : "M"}${p[0]},${p[1]}`).join(" ");
  const area = `${line} L${x(n - 1)},${padT + ih} L${x(0)},${padT + ih} Z`;

  return (
    <div ref={ref} className="relative w-full">
      <svg width={W} height={H} viewBox={`0 0 ${W} ${H}`} style={{ display: "block" }}
        onMouseLeave={() => setHi(null)}
        onMouseMove={(e) => {
          const r = (e.currentTarget as SVGSVGElement).getBoundingClientRect();
          const rel = ((e.clientX - r.left) / r.width) * W;
          let best = 0, bd = Infinity;
          pts.forEach((p, i) => { const d = Math.abs(p[0] - rel); if (d < bd) { bd = d; best = i; } });
          setHi(best);
        }}>
        <defs>
          <linearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.22" />
            <stop offset="100%" stopColor="var(--primary)" stopOpacity="0.01" />
          </linearGradient>
        </defs>
        {/* 가로 그리드 3줄 */}
        {[0.25, 0.5, 0.75].map((g) => (
          <line key={g} x1={padL} x2={W - padR} y1={padT + ih * g} y2={padT + ih * g}
            stroke={GRID} strokeWidth="1" strokeDasharray="3 4" opacity="0.6" />
        ))}
        <path d={area} fill="url(#areaFill)" />
        <path d={line} fill="none" stroke="var(--primary)" strokeWidth="2.5" strokeLinejoin="round" strokeLinecap="round" />
        {hi !== null && (
          <line x1={pts[hi][0]} x2={pts[hi][0]} y1={padT} y2={padT + ih} stroke="var(--primary)" strokeWidth="1" opacity="0.5" />
        )}
        {pts.map((p, i) => (
          <circle key={i} cx={p[0]} cy={p[1]} r={hi === i ? 5 : 3.5}
            fill="var(--card)" stroke="var(--primary)" strokeWidth="2.5" />
        ))}
        {data.map((d, i) => (
          <text key={i}
            x={i === 0 ? padL : i === n - 1 ? W - padR : x(i)}
            y={H - 8}
            textAnchor={i === 0 ? "start" : i === n - 1 ? "end" : "middle"}
            fontSize="11" fill={AXIS}>{d.label}</text>
        ))}
      </svg>
      {hi !== null && (
        <div className="pointer-events-none absolute -translate-x-1/2 -translate-y-full rounded-lg bg-popover px-2.5 py-1.5 text-xs font-bold text-popover-foreground shadow-[var(--shadow-pop)] tnum"
          style={{ left: `${(pts[hi][0] / W) * 100}%`, top: `${(pts[hi][1] / H) * 100}%` }}>
          {data[hi].label} · {formatKRW(data[hi].value)}
        </div>
      )}
    </div>
  );
}

/** 월별 건수 — 세로 막대(단일 hue). 데이터끝 4px 라운드, 2px 간격, 호버 툴팁. */
export function BarTrend({ data, unit = "건", height = 180 }: { data: Pt[]; unit?: string; height?: number }) {
  const max = Math.max(...data.map((d) => d.value), 1);
  return (
    <div className="flex items-end gap-2" style={{ height }}>
      {data.map((d) => {
        const h = Math.max(6, Math.round((d.value / (max)) * (height - 28)));
        return (
          <div key={d.label} className="group relative flex flex-1 flex-col items-center justify-end" style={{ height }}>
            <div className="pointer-events-none absolute z-10 -translate-y-1 whitespace-nowrap rounded-lg bg-popover px-2 py-1 text-[11px] font-bold text-popover-foreground opacity-0 shadow-[var(--shadow-pop)] transition-opacity group-hover:opacity-100 tnum"
              style={{ bottom: h + 22 }}>
              {d.value.toLocaleString()}{unit}
            </div>
            <div className="w-full max-w-[38px] rounded-t-[4px] bg-primary/85 transition-all duration-500 group-hover:bg-primary"
              style={{ height: h }} />
            <span className="mt-1.5 text-[11px] text-muted-foreground">{d.label}</span>
          </div>
        );
      })}
    </div>
  );
}

/** 상태 분포 — 도넛(예약 상태색) + 범례. 중앙에 합계. */
export function Donut({ slices, unit = "", size = 148 }: { slices: Slice[]; unit?: string; size?: number }) {
  const total = slices.reduce((s, x) => s + x.value, 0);
  const r = size / 2 - 12, cx = size / 2, cy = size / 2, C = 2 * Math.PI * r;
  let acc = 0;
  return (
    <div className="flex items-center gap-5">
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="shrink-0 -rotate-90">
        <circle cx={cx} cy={cy} r={r} fill="none" stroke="var(--border)" strokeWidth="12" opacity="0.5" />
        {total > 0 && slices.map((s, i) => {
          const frac = s.value / total;
          const seg = (
            <circle key={i} cx={cx} cy={cy} r={r} fill="none" stroke={s.color} strokeWidth="12"
              strokeDasharray={`${frac * C - 2} ${C}`} strokeDashoffset={-acc * C} strokeLinecap="round" />
          );
          acc += frac;
          return s.value > 0 ? seg : null;
        })}
        <text x={cx} y={cy} transform={`rotate(90 ${cx} ${cy})`} textAnchor="middle" dominantBaseline="central"
          fontSize="22" fontWeight="800" fill="var(--foreground)">{total}</text>
      </svg>
      <ul className="flex flex-col gap-1.5 text-sm">
        {slices.map((s) => (
          <li key={s.name} className="flex items-center gap-2">
            <span className="size-2.5 shrink-0 rounded-full" style={{ background: s.color }} />
            <span className="text-muted-foreground">{s.name}</span>
            <span className="ml-auto font-bold tnum">{s.value.toLocaleString()}{unit}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

/** 항목별 값 — 가로 막대(단일 hue) + 직접 값 라벨. */
export function HBars({ data, money = false }: { data: Pt[]; money?: boolean }) {
  const max = Math.max(...data.map((d) => d.value), 1);
  const fmt = (v: number) => (money ? formatKRW(v) : v.toLocaleString());
  if (data.length === 0) return <div className="py-8 text-center text-sm text-muted-foreground">데이터가 없습니다.</div>;
  return (
    <ul className="flex flex-col gap-3">
      {data.map((d) => (
        <li key={d.label} className="grid grid-cols-[7rem_1fr_auto] items-center gap-3 text-sm">
          <span className="truncate text-muted-foreground">{d.label}</span>
          <span className="h-2.5 rounded-full bg-muted">
            <span className="block h-full rounded-full bg-primary transition-all duration-500"
              style={{ width: `${Math.max(3, (d.value / max) * 100)}%` }} />
          </span>
          <span className="text-right font-bold tnum">{fmt(d.value)}</span>
        </li>
      ))}
    </ul>
  );
}
