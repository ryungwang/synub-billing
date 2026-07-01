package io.synub.billing.service;

import io.synub.billing.auth.AuthContext;
import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Membership;
import io.synub.billing.web.ApiExceptions.ForbiddenException;
import org.springframework.stereotype.Component;

/**
 * 현재 요청의 소유 스코프({@link Owner}) 결정 + 접근 통제.
 * 개인 컨텍스트 → 본인(customer) 소유. 조직 컨텍스트 → 멤버십 검증(미소속 403).
 * 쓰기(카드 등록·구독 생성/변경)는 조직의 결제 관리 권한(owner/billing_manager)을 추가로 요구한다.
 */
@Component
public class CurrentScope {

    private final CurrentUser currentUser;
    private final OrganizationService organizations;

    public CurrentScope(CurrentUser currentUser, OrganizationService organizations) {
        this.currentUser = currentUser;
        this.organizations = organizations;
    }

    /** 조회용 소유 스코프. 조직이면 멤버십만 있으면 된다(멤버도 조회 가능). */
    public Owner readOwner() {
        AuthContext ctx = currentUser.context();
        Customer me = currentUser.resolve();
        if (!ctx.isOrg()) return Owner.customer(me.getId());
        organizations.membership(ctx.orgId(), me.getId())
                .orElseThrow(() -> new ForbiddenException("해당 조직에 대한 접근 권한이 없습니다."));
        return Owner.organization(ctx.orgId());
    }

    /** 쓰기용 소유 스코프. 조직이면 결제 관리 권한(owner/billing_manager)이 필요하다. */
    public Owner writeOwner() {
        AuthContext ctx = currentUser.context();
        Customer me = currentUser.resolve();
        if (!ctx.isOrg()) return Owner.customer(me.getId());
        Membership m = organizations.membership(ctx.orgId(), me.getId())
                .orElseThrow(() -> new ForbiddenException("해당 조직에 대한 접근 권한이 없습니다."));
        if (!m.canManageBilling()) {
            throw new ForbiddenException("이 작업은 조직의 소유자·결제 관리자만 할 수 있습니다.");
        }
        // 사업자 인증 미완료(pending/rejected) 회사는 결제·구독 등 쓰기 차단(도용 방지)
        if (!organizations.org(ctx.orgId()).isVerified()) {
            throw new ForbiddenException("회사 인증이 완료되어야 결제·구독을 이용할 수 있습니다.");
        }
        return Owner.organization(ctx.orgId());
    }
}
