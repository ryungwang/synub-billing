"use client";

import * as React from "react";
import { Package, Plus, Pencil, Loader2, EyeOff } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";
import { api, type ApiAdminProduct, type ProductMetaInput } from "@/lib/api";

const FIELD =
  "w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none transition-shadow focus:ring-2 focus:ring-ring/30";

/**
 * 관리자 제품 카탈로그 — 메타(이름·설명·URL·정렬·노출)만 편집. 플랜·가격(돈)은 마이그레이션 전용.
 */
export function AdminProducts() {
  const [items, setItems] = React.useState<ApiAdminProduct[] | null>(null);
  const [editing, setEditing] = React.useState<ApiAdminProduct | "new" | null>(null);

  const load = React.useCallback(() => {
    api.adminProducts().then(setItems).catch(() => setItems([]));
  }, []);
  React.useEffect(() => load(), [load]);

  return (
    <Card className="mt-6 p-0">
      <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-3.5">
        <div className="flex items-center gap-2 text-sm font-bold">
          <Package className="size-4 text-muted-foreground" />
          제품 카탈로그 {items ? <span className="text-muted-foreground">{items.length}</span> : null}
        </div>
        <Button size="sm" onClick={() => setEditing("new")}>
          <Plus /> 제품 추가
        </Button>
      </div>
      <p className="border-b border-border bg-muted/30 px-5 py-2 text-xs text-muted-foreground">
        메타(이름·설명·URL·정렬·노출)만 여기서 관리합니다. <b>플랜·가격은 마이그레이션 전용</b>이라 이 화면에서 바꿀 수 없습니다.
      </p>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border text-left text-xs text-muted-foreground">
              <th className="px-5 py-2.5 font-medium">제품</th>
              <th className="px-3 py-2.5 font-medium">service_code</th>
              <th className="px-3 py-2.5 font-medium">분류</th>
              <th className="px-3 py-2.5 font-medium">플랜</th>
              <th className="px-3 py-2.5 font-medium">정렬</th>
              <th className="px-3 py-2.5 font-medium">상태</th>
              <th className="px-5 py-2.5" />
            </tr>
          </thead>
          <tbody>
            {items?.map((p) => (
              <tr key={p.id} className="border-b border-border last:border-0 hover:bg-muted/40">
                <td className="px-5 py-3">
                  <div className="font-medium">{p.name}</div>
                  {p.description && (
                    <div className="mt-0.5 max-w-md truncate text-xs text-muted-foreground">{p.description}</div>
                  )}
                </td>
                <td className="px-3 py-3 font-mono text-xs">{p.serviceCode}</td>
                <td className="px-3 py-3 text-muted-foreground">
                  {p.category ?? "—"}
                  {p.orgOnly && <Badge className="ml-1.5 align-middle text-[10px]">조직전용</Badge>}
                </td>
                <td className="px-3 py-3 tnum text-muted-foreground">{p.planCount}</td>
                <td className="px-3 py-3 tnum text-muted-foreground">{p.sortOrder}</td>
                <td className="px-3 py-3">
                  {p.status === "active" ? (
                    <Badge variant="success">노출</Badge>
                  ) : p.status === "coming_soon" ? (
                    <Badge variant="outline" className="border-primary/40 text-primary">준비중</Badge>
                  ) : (
                    <Badge variant="outline">
                      <EyeOff className="size-3" /> 숨김
                    </Badge>
                  )}
                </td>
                <td className="px-5 py-3 text-right">
                  <Button variant="ghost" size="sm" onClick={() => setEditing(p)}>
                    <Pencil /> 편집
                  </Button>
                </td>
              </tr>
            ))}
            {items && items.length === 0 && (
              <tr>
                <td colSpan={7} className="px-5 py-10 text-center text-sm text-muted-foreground">
                  등록된 제품이 없습니다. “제품 추가”로 시작하세요.
                </td>
              </tr>
            )}
            {!items && (
              <tr>
                <td colSpan={7} className="px-5 py-10 text-center text-muted-foreground">
                  <Loader2 className="mx-auto size-5 animate-spin" />
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {editing && (
        <ProductForm
          product={editing === "new" ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            load();
          }}
        />
      )}
    </Card>
  );
}

function ProductForm({
  product,
  onClose,
  onSaved,
}: {
  product: ApiAdminProduct | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const isNew = product === null;
  const [f, setF] = React.useState<ProductMetaInput>({
    serviceCode: product?.serviceCode ?? "",
    name: product?.name ?? "",
    category: product?.category ?? "",
    description: product?.description ?? "",
    domainUrl: product?.domainUrl ?? "",
    demoUrl: product?.demoUrl ?? "",
    onboardingUrl: product?.onboardingUrl ?? "",
    webhookUrl: product?.webhookUrl ?? "",
    sortOrder: product?.sortOrder ?? 0,
    orgOnly: product?.orgOnly ?? false,
    status: product?.status ?? "active",
  });
  const [saving, setSaving] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const set = <K extends keyof ProductMetaInput>(k: K, v: ProductMetaInput[K]) => setF((s) => ({ ...s, [k]: v }));

  async function save() {
    setError(null);
    if (!f.name?.trim()) return setError("제품명을 입력하세요.");
    if (isNew && !/^[a-z0-9-]{2,50}$/.test(f.serviceCode?.trim() ?? ""))
      return setError("service_code는 소문자-케밥(a-z,0-9,-) 2~50자여야 합니다.");
    setSaving(true);
    try {
      if (isNew) await api.adminCreateProduct(f);
      else await api.adminUpdateProduct(product!.id, f);
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장에 실패했습니다.");
      setSaving(false);
    }
  }

  return (
    <Dialog open onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-xl">
        <DialogHeader>
          <DialogTitle>{isNew ? "제품 추가" : "제품 편집"}</DialogTitle>
          <DialogDescription>
            메타데이터만 관리합니다. 플랜·가격은 마이그레이션으로 등록하세요.
          </DialogDescription>
        </DialogHeader>

        <div className="grid max-h-[60vh] gap-3.5 overflow-y-auto pr-1">
          <label className="grid gap-1.5">
            <span className="text-xs font-medium text-muted-foreground">service_code {isNew && "*"}</span>
            <input
              className={FIELD + (isNew ? "" : " opacity-60")}
              value={f.serviceCode ?? ""}
              onChange={(e) => set("serviceCode", e.target.value)}
              placeholder="office / doc-analysis"
              disabled={!isNew}
            />
            {!isNew && <span className="text-[11px] text-muted-foreground">연동 키라 변경 불가(불변).</span>}
          </label>

          <label className="grid gap-1.5">
            <span className="text-xs font-medium text-muted-foreground">제품명 *</span>
            <input className={FIELD} value={f.name} onChange={(e) => set("name", e.target.value)} placeholder="OOffice" />
          </label>

          <div className="grid grid-cols-2 gap-3.5">
            <label className="grid gap-1.5">
              <span className="text-xs font-medium text-muted-foreground">분류</span>
              <input className={FIELD} value={f.category ?? ""} onChange={(e) => set("category", e.target.value)} placeholder="그룹웨어" />
            </label>
            <label className="grid gap-1.5">
              <span className="text-xs font-medium text-muted-foreground">정렬 순서</span>
              <input type="number" className={FIELD} value={f.sortOrder ?? 0} onChange={(e) => set("sortOrder", Number(e.target.value))} />
            </label>
          </div>

          <label className="grid gap-1.5">
            <span className="text-xs font-medium text-muted-foreground">설명</span>
            <textarea className={FIELD} rows={2} value={f.description ?? ""} onChange={(e) => set("description", e.target.value)} placeholder="카드에 노출되는 한 줄 설명" />
          </label>

          <label className="grid gap-1.5">
            <span className="text-xs font-medium text-muted-foreground">제품 URL (domain)</span>
            <input className={FIELD} value={f.domainUrl ?? ""} onChange={(e) => set("domainUrl", e.target.value)} placeholder="https://office.synub.io" />
          </label>
          <label className="grid gap-1.5">
            <span className="text-xs font-medium text-muted-foreground">데모(체험) URL</span>
            <input className={FIELD} value={f.demoUrl ?? ""} onChange={(e) => set("demoUrl", e.target.value)} placeholder="https://office.synub.io/demo" />
          </label>
          <label className="grid gap-1.5">
            <span className="text-xs font-medium text-muted-foreground">온보딩(초기설정) URL</span>
            <input className={FIELD} value={f.onboardingUrl ?? ""} onChange={(e) => set("onboardingUrl", e.target.value)} placeholder="https://office.synub.io/onboarding" />
          </label>
          <label className="grid gap-1.5">
            <span className="text-xs font-medium text-muted-foreground">웹훅 수신 URL</span>
            <input className={FIELD} value={f.webhookUrl ?? ""} onChange={(e) => set("webhookUrl", e.target.value)} placeholder="https://office.synub.io/webhooks/billing" />
          </label>

          <div className="flex flex-wrap items-center gap-x-5 gap-y-3 pt-1">
            <label className="flex min-w-0 items-center gap-2 text-sm">
              <input type="checkbox" className="size-4 shrink-0 accent-primary" checked={!!f.orgOnly} onChange={(e) => set("orgOnly", e.target.checked)} />
              조직 전용 (회사 컨텍스트에서만 구독)
            </label>
            <label className="flex shrink-0 items-center gap-2 text-sm">
              <span className="whitespace-nowrap text-muted-foreground">노출</span>
              <Select value={f.status ?? "active"} onValueChange={(v) => set("status", v)}>
                <SelectTrigger className="h-9 w-32">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="active">노출</SelectItem>
                  <SelectItem value="coming_soon">준비중</SelectItem>
                  <SelectItem value="inactive">숨김</SelectItem>
                </SelectContent>
              </Select>
            </label>
          </div>

          {error && <p className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={saving}>취소</Button>
          <Button onClick={save} disabled={saving}>
            {saving ? <Loader2 className="animate-spin" /> : null}
            {isNew ? "등록" : "저장"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
