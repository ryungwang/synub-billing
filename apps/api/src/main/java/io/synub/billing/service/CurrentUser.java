package io.synub.billing.service;

import io.synub.billing.auth.AuthContext;
import io.synub.billing.auth.Identity;
import io.synub.billing.auth.IdentityContext;
import io.synub.billing.config.AppProperties;
import io.synub.billing.domain.Customer;
import io.synub.billing.repo.CustomerRepository;
import io.synub.billing.tenant.CurrentTenant;
import org.springframework.stereotype.Component;

/**
 * 현재 로그인 고객 resolver. SSO 신원({@link IdentityContext})의 external_id 로 고객을 찾는다.
 * 요청 밖(스케줄러 등)이거나 dev-fallback 로컬 환경에서는 설정된 데모 external_id 로 폴백한다.
 */
@Component
public class CurrentUser {

    /** 호환용 상수(구코드 참조). 실제 폴백 값은 app.sso.dev-external-id. */
    public static final String DEMO_EXTERNAL_ID = "demo-user";

    private final CustomerRepository customers;
    private final CurrentTenant tenant;
    private final CustomerProvisioning provisioning;
    private final AppProperties.Sso sso;

    public CurrentUser(CustomerRepository customers, CurrentTenant tenant,
                       CustomerProvisioning provisioning, AppProperties props) {
        this.customers = customers;
        this.tenant = tenant;
        this.provisioning = provisioning;
        this.sso = props.sso();
    }

    /** 현재 요청의 external_id. 신원이 없으면 dev 폴백 external_id. */
    public String externalId() {
        Identity id = IdentityContext.current();
        if (id != null && id.externalId() != null && !id.externalId().isBlank()) {
            return id.externalId();
        }
        return (sso.devExternalId() != null && !sso.devExternalId().isBlank())
                ? sso.devExternalId() : DEMO_EXTERNAL_ID;
    }

    /** 현재 요청의 이메일(토큰 claim). 신원이 없으면 dev 폴백 이메일. */
    public String email() {
        Identity id = IdentityContext.current();
        return (id != null && id.email() != null) ? id.email() : sso.devEmail();
    }

    /** 현재 요청의 컨텍스트(개인/조직). 신원이 없으면 개인. */
    public AuthContext context() {
        Identity id = IdentityContext.current();
        return id != null ? id.context() : AuthContext.personal();
    }

    /** 플랫폼 관리자 여부(토큰 admin claim). */
    public boolean isAdmin() {
        Identity id = IdentityContext.current();
        return id != null && id.admin();
    }

    /**
     * 현재 고객. SSO 신규 사용자면 JIT provisioning 으로 자동 생성해 반환한다
     * (검증된 토큰이면 처음 보는 사용자라도 빌링 고객으로 확립).
     */
    public Customer resolve() {
        String externalId = externalId();
        return customers.findByCompanyIdAndExternalId(tenant.companyId(), externalId)
                .orElseGet(() -> provisioning.ensure(externalId, email()));
    }
}
