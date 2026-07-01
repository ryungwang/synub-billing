package io.synub.billing.service;

import io.synub.billing.domain.Customer;
import io.synub.billing.repo.CustomerRepository;
import io.synub.billing.tenant.CurrentTenant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * SSO 신규 사용자의 billing customer 자동 생성(JIT provisioning).
 * SSO가 신원의 진실이므로, 토큰이 검증되면 빌링은 처음 보는 external_id 라도 고객 레코드를 만들어 붙인다.
 * REQUIRES_NEW: 조회(readOnly) 트랜잭션 안에서 호출돼도 별도 트랜잭션으로 커밋되도록.
 */
@Service
public class CustomerProvisioning {

    private final CustomerRepository customers;
    private final CurrentTenant tenant;

    public CustomerProvisioning(CustomerRepository customers, CurrentTenant tenant) {
        this.customers = customers;
        this.tenant = tenant;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Customer ensure(String externalId, String email) {
        Long companyId = tenant.companyId();
        return customers.findByCompanyIdAndExternalId(companyId, externalId)
                .orElseGet(() -> insert(companyId, externalId, email));
    }

    private Customer insert(Long companyId, String externalId, String email) {
        try {
            return customers.save(new Customer(companyId, externalId, email));
        } catch (DataIntegrityViolationException race) {
            // 동시 첫 요청 경합 — 유니크 제약 충돌 시 이미 생성된 레코드를 다시 조회
            return customers.findByCompanyIdAndExternalId(companyId, externalId)
                    .orElseThrow(() -> race);
        }
    }
}
