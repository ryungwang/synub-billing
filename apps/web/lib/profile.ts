// 현재 사용자 아바타 URL 공유 스토어 — 마이페이지·우상단 아바타가 같은 값을 구독.
// 업로드/삭제 시 setAvatarUrl로 전역 반영, loadAvatarOnce로 최초 1회만 서버 조회.
import { api } from "./api";

let current: string | null = null;
let started = false;
const listeners = new Set<() => void>();

export function subscribeAvatar(cb: () => void) {
  listeners.add(cb);
  return () => listeners.delete(cb);
}

/** useSyncExternalStore 스냅샷. */
export function avatarSnapshot(): string | null {
  return current;
}

export function setAvatarUrl(url: string | null) {
  current = url;
  listeners.forEach((l) => l());
}

/** 최초 1회 서버에서 아바타 로드(중복 호출 방지). */
export function loadAvatarOnce() {
  if (started) return;
  started = true;
  api
    .getProfile()
    .then((p) => setAvatarUrl(p.avatarUrl))
    .catch(() => {});
}
