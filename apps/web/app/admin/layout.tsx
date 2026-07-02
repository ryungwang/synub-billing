"use client";

import * as React from "react";
import { ShieldAlert } from "lucide-react";
import { PageHeader } from "@/components/page-header";
import { EmptyState } from "@/components/empty-state";
import { useAuth } from "@/lib/auth";

/** 관리자 콘솔 공통 게이트 — 비관리자 접근 차단. 하위 페이지(대시보드·구독·결제·회사심사·제품)를 감싼다. */
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  if (user && !user.admin) {
    return (
      <>
        <PageHeader title="관리자" description="플랫폼 운영 콘솔" />
        <EmptyState
          icon={ShieldAlert}
          title="관리자 전용 페이지입니다"
          description="이 페이지에 접근할 권한이 없습니다."
        />
      </>
    );
  }
  return <>{children}</>;
}
