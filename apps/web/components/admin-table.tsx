"use client";

import * as React from "react";
import { Loader2 } from "lucide-react";

/** 관리자 콘솔 공용 테이블 — 로딩/빈 상태 처리 포함. */
export function Table<T>({
  rows,
  cols,
  render,
}: {
  rows: T[] | null;
  cols: string[];
  render: (row: T) => React.ReactNode;
}) {
  if (rows === null) {
    return (
      <div className="flex items-center justify-center gap-2 py-12 text-muted-foreground">
        <Loader2 className="size-5 animate-spin" /> 불러오는 중…
      </div>
    );
  }
  if (rows.length === 0) {
    return <div className="py-10 text-center text-sm text-muted-foreground">데이터가 없습니다.</div>;
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border text-left text-[11px] font-bold uppercase tracking-wide text-muted-foreground">
            {cols.map((c, i) => (
              <th key={i} className="px-5 py-2.5 font-bold">
                {c}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{rows.map(render)}</tbody>
      </table>
    </div>
  );
}

export function Td({ children, className }: { children: React.ReactNode; className?: string }) {
  return <td className={"px-5 py-3 " + (className ?? "")}>{children}</td>;
}
