/** 약관/개인정보/환불 등 법적 문서 공통 레이아웃 — 읽기 좋은 폭 + prose 스타일. */
export function Legal({
  title,
  effective,
  intro,
  children,
}: {
  title: string;
  effective: string;
  intro?: string;
  children: React.ReactNode;
}) {
  return (
    <article className="mx-auto max-w-3xl">
      <h1 className="text-2xl font-extrabold tracking-tight">{title}</h1>
      <p className="mt-2 text-sm text-muted-foreground">시행일 {effective}</p>
      {intro && <p className="mt-4 text-sm leading-relaxed text-muted-foreground">{intro}</p>}
      <div
        className="mt-8 space-y-3 text-sm leading-relaxed text-secondary-foreground
          [&_h2]:mt-8 [&_h2]:text-base [&_h2]:font-bold [&_h2]:text-foreground
          [&_p]:mt-2 [&_ul]:mt-2 [&_ul]:list-disc [&_ul]:space-y-1 [&_ul]:pl-5"
      >
        {children}
      </div>
    </article>
  );
}
