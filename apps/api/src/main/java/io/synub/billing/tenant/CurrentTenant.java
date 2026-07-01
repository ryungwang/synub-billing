package io.synub.billing.tenant;

import io.synub.billing.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * 현재 테넌트(company_id) 제공자. MVP는 단일 법인이라 설정값 고정.
 * 추후 SSO/요청 컨텍스트에서 동적으로 주입하도록 확장.
 */
@Component
public class CurrentTenant {

    private final long defaultCompanyId;

    public CurrentTenant(AppProperties props) {
        this.defaultCompanyId = props.tenant().defaultCompanyId();
    }

    public long companyId() {
        return defaultCompanyId;
    }
}
