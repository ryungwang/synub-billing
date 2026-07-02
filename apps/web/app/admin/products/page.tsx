"use client";

import { PageHeader } from "@/components/page-header";
import { AdminProducts } from "@/components/admin-products";

export default function AdminProductsPage() {
  return (
    <>
      <PageHeader
        title="관리자 · 제품 카탈로그"
        description="제품 메타(이름·설명·URL·정렬·노출)를 관리합니다. 플랜·가격은 마이그레이션으로 통제."
      />
      <AdminProducts />
    </>
  );
}
