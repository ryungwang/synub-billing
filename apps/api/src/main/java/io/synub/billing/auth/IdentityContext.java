package io.synub.billing.auth;

/**
 * 요청 스레드에 묶인 현재 신원 보관소(ThreadLocal). {@link IdentityFilter}가 요청 시작 시 set,
 * 종료 시 clear 한다. 서비스 계층은 {@code io.synub.billing.service.CurrentUser}를 통해 접근한다.
 * (스케줄러 등 요청 밖 컨텍스트에서는 current()가 null → 호출측이 fail-closed 처리)
 */
public final class IdentityContext {

    private static final ThreadLocal<Identity> HOLDER = new ThreadLocal<>();

    private IdentityContext() {}

    public static void set(Identity identity) {
        HOLDER.set(identity);
    }

    public static Identity current() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
