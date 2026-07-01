package io.synub.billing.service;

import io.synub.billing.auth.AuthContext;
import io.synub.billing.domain.Customer;
import io.synub.billing.web.ApiExceptions.ForbiddenException;
import org.springframework.stereotype.Component;

/**
 * 현재 요청의 데이터 스코프 결정 + 접근 통제.
 * 개인 컨텍스트 → 본인(customer) 스코프. 조직 컨텍스트 → 멤버십 검증(미소속이면 403).
 *
 * <p>주의(데이터 격리): 조직 소유 구독/카드는 아직 미구현(다음 마일스톤)이라, 조직 컨텍스트의 조회는
 * 본인 개인 데이터를 노출하지 않고 <b>빈 결과</b>를 반환해야 한다. {@link #isOrgContext()}로 판별한다.
 */
@Component
public class CurrentScope {

    private final CurrentUser currentUser;
    private final OrganizationService organizations;

    public CurrentScope(CurrentUser currentUser, OrganizationService organizations) {
        this.currentUser = currentUser;
        this.organizations = organizations;
    }

    /** 조직 컨텍스트인가. */
    public boolean isOrgContext() {
        return currentUser.context().isOrg();
    }

    /**
     * 조직 컨텍스트면 멤버십을 검증하고 orgId 반환(미소속이면 403), 개인 컨텍스트면 null.
     * 조회 서비스는 반환값이 non-null 이면 개인 데이터 대신 빈 결과를 돌려줘야 한다.
     */
    public Long enforceOrgContext() {
        AuthContext ctx = currentUser.context();
        if (!ctx.isOrg()) return null;
        Customer me = currentUser.resolve();
        organizations.membership(ctx.orgId(), me.getId())
                .orElseThrow(() -> new ForbiddenException("해당 조직에 대한 접근 권한이 없습니다."));
        return ctx.orgId();
    }
}
