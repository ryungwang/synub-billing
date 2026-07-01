// 토큰 저장소 — 클라이언트 경계("use client") 없는 순수 모듈이라 서버/클라 어디서 import 해도 안전.
// (브라우저 API는 typeof window 가드로 서버에선 no-op/ null 반환)

export const TOKEN_KEY = "synub_token";

export interface AuthUser {
  sub: string;
  email: string;
  name?: string;
  exp: number;
}

const listeners = new Set<() => void>();
export function subscribeToken(cb: () => void) {
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

/** JWT payload 디코딩(서명검증 없음 — 표시/만료확인 전용). */
export function decode(token: string): AuthUser | null {
  try {
    const part = token.split(".")[1];
    const b64 = part
      .replace(/-/g, "+")
      .replace(/_/g, "/")
      .padEnd(part.length + ((4 - (part.length % 4)) % 4), "=");
    const bytes = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
    const json = JSON.parse(new TextDecoder().decode(bytes));
    return { sub: json.sub, email: json.email, name: json.name, exp: json.exp };
  } catch {
    return null;
  }
}

/** 원시 토큰 문자열(만료 검사 안 함). useSyncExternalStore 스냅샷용. */
export function rawToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

/** 유효한(미만료) 토큰만 반환. 만료 시 자동 제거. */
export function getToken(): string | null {
  const t = rawToken();
  if (!t) return null;
  const u = decode(t);
  if (!u || u.exp * 1000 <= Date.now()) {
    clearToken();
    return null;
  }
  return t;
}

export function getUser(): AuthUser | null {
  const t = getToken();
  return t ? decode(t) : null;
}

export function setToken(token: string) {
  if (typeof window !== "undefined") localStorage.setItem(TOKEN_KEY, token);
  emit();
}

export function clearToken() {
  if (typeof window !== "undefined") localStorage.removeItem(TOKEN_KEY);
  emit();
}
