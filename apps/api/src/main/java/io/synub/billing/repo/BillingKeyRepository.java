package io.synub.billing.repo;

import io.synub.billing.domain.BillingKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {
    List<BillingKey> findByCustomerIdAndStatusOrderByCreatedAtAsc(Long customerId, String status);
    Optional<BillingKey> findByIdAndCustomerId(Long id, Long customerId);
    List<BillingKey> findByCustomerId(Long customerId);
}
