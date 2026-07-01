"use client";

// 통합계정(SSO) 로그인/가입 + 로그인 상태 훅. 토큰 저장 자체는 lib/token.ts(순수 모듈)에 위임.

import { useSyncExternalStore } from "react";
import {
  type AuthUser,
  decode,
  rawToken,
  setToken,
  clearToken,
  subscribeToken,
} from "./token";
import { resetContext } from "./context";

export type { AuthUser };

const SSO_BASE = process.env.NEXT_PUBLIC_SSO_BASE_URL ?? "http://localhost:8090";

export async function login(email: string, password: string): Promise<void> {
  const res = await fetch(`${SSO_BASE}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) {
    const b = await res.json().catch(() => ({}));
    throw new Error(b.message ?? "로그인에 실패했습니다.");
  }
  const data = await res.json();
  setToken(data.accessToken);
}

export async function register(
  email: string,
  password: string,
  name: string
): Promise<void> {
  const res = await fetch(`${SSO_BASE}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password, name }),
  });
  if (!res.ok) {
    const b = await res.json().catch(() => ({}));
    throw new Error(b.message ?? "가입에 실패했습니다.");
  }
  await login(email, password); // 가입 직후 자동 로그인
}

export function logout() {
  resetContext(); // 로그아웃 시 개인 컨텍스트로 초기화
  clearToken();
}

/** 로그인 상태 훅. 토큰 변화에 반응해 현재 사용자(미만료)만 반환. */
export function useAuth() {
  const token = useSyncExternalStore(subscribeToken, rawToken, () => null);
  const user = token ? decode(token) : null;
  const valid = user && user.exp * 1000 > Date.now() ? user : null;
  return { user: valid, login, register, logout };
}
