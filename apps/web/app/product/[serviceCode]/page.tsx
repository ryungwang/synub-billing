import { notFound } from "next/navigation";
import type { Metadata } from "next";
import type { CatalogProduct } from "@/components/plan-grid";
import { ProductDetail } from "./product-detail";

// 공개 제품 상세 — 카탈로그(/products)에서 서버 렌더(검색 노출·비로그인 열람). 데이터 기반이라 제품별 코드 불필요.
export const dynamic = "force-dynamic";

async function getProduct(code: string): Promise<CatalogProduct | null> {
  const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  try {
    const res = await fetch(`${base}/products`, { cache: "no-store" });
    if (!res.ok) return null;
    const list = (await res.json()) as CatalogProduct[];
    return list.find((p) => p.serviceCode === code) ?? null;
  } catch {
    return null;
  }
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ serviceCode: string }>;
}): Promise<Metadata> {
  const { serviceCode } = await params;
  const product = await getProduct(serviceCode);
  if (!product) return { title: "제품을 찾을 수 없습니다 — Synub Billing" };
  return {
    title: `${product.name} — Synub Billing`,
    description: product.description ?? undefined,
  };
}

export default async function ProductPage({
  params,
}: {
  params: Promise<{ serviceCode: string }>;
}) {
  const { serviceCode } = await params;
  const product = await getProduct(serviceCode);
  if (!product) notFound();
  return <ProductDetail product={product} />;
}
