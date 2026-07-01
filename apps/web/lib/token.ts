// 토큰 저장소 — 클라이언트 경계("use client") 없는 순수 모듈이라 서버/클라 어디서 import 해도 안전.
// (브라우저 API는 typeof window 가드로 서버에선 no-op/ null 반환)

export const TOKEN_KEY = "synub_token";
export const REFRESH_KEY = "synub_refresh";
const SSO_BASE = process.env.NEXT_PUBLIC_SSO_BASE_URL ?? "http://localhost:8090";

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

/**
 * 유효한(미만료) 액세스 토큰만 반환. 만료/무효면 null.
 * 여기서 저장소를 지우지 않는다 — 리프레시 토큰으로 갱신할 기회를 남겨야 하므로.
 */
export function getToken(): string | null {
  const t = rawToken();
  if (!t) return null;
  const u = decode(t);
  if (!u || u.exp * 1000 <= Date.now()) return null;
  return t;
}

export function getUser(): AuthUser | null {
  const t = getToken();
  return t ? decode(t) : null;
}

export function getRefreshToken(): string | null {
  return typeof window !== "undefined" ? localStorage.getItem(REFRESH_KEY) : null;
}

/** 로그인/갱신 응답의 access(+refresh) 저장. */
export function setTokens(accessToken: string, refreshToken?: string) {
  if (typeof window === "undefined") return;
  localStorage.setItem(TOKEN_KEY, accessToken);
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken);
  emit();
}

export function clearToken() {
  if (typeof window !== "undefined") {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
  }
  emit();
}

let refreshing: Promise<string | null> | null = null;

/**
 * 리프레시 토큰으로 액세스 토큰 갱신. 동시 호출은 하나로 합친다(중복 회전 방지).
 * 성공 시 새 토큰 저장 후 access 반환, 실패 시 토큰 정리 후 null.
 */
export function refreshAccessToken(): Promise<string | null> {
  if (typeof window === "undefined") return Promise.resolve(null);
  const rt = getRefreshToken();
  if (!rt) return Promise.resolve(null);
  if (refreshing) return refreshing;

  refreshing = fetch(`${SSO_BASE}/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken: rt }),
  })
    .then(async (res) => {
      if (!res.ok) {
        clearToken();
        return null;
      }
      const data = await res.json();
      setTokens(data.accessToken, data.refreshToken);
      return data.accessToken as string;
    })
    .catch(() => {
      clearToken();
      return null;
    })
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
}
