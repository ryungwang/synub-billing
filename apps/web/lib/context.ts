// 현재 컨텍스트(개인/회사) 저장 — 순수 모듈("use client" 없음, 서버 import 안전).
// api 호출 시 X-Synub-Context 헤더로 전송된다: "personal" | "org:{id}".

const CTX_KEY = "synub_context";

const listeners = new Set<() => void>();
export function subscribeContext(cb: () => void) {
  listeners.add(cb);
  if (typeof window !== "undefined") window.addEventListener("storage", cb);
  return () => {
    listeners.delete(cb);
    if (typeof window !== "undefined") window.removeEventListener("storage", cb);
  };
}
function emit() {
  listeners.forEach((l) => l());
}

/** X-Synub-Context 헤더값. 기본 personal. */
export function getContextHeader(): string {
  if (typeof window === "undefined") return "personal";
  return localStorage.getItem(CTX_KEY) || "personal";
}

/** useSyncExternalStore 스냅샷용. */
export function rawContext(): string {
  return getContextHeader();
}

export function setContext(value: string) {
  if (typeof window !== "undefined") localStorage.setItem(CTX_KEY, value);
  emit();
}

export function resetContext() {
  if (typeof window !== "undefined") localStorage.removeItem(CTX_KEY);
  emit();
}

/** "personal" → null, "org:{id}" → id */
export function contextOrgId(value: string): number | null {
  if (value.startsWith("org:")) {
    const n = Number(value.slice(4));
    return Number.isFinite(n) ? n : null;
  }
  return null;
}
