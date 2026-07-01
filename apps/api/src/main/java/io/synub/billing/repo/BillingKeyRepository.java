package io.synub.billing.repo;

import io.synub.billing.domain.BillingKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {
    List<BillingKey> findByCustomerIdAndStatusOrderByCreatedAtAsc(Long customerId, String status);
    Optional<BillingKey> findByIdAndCustomerId(Long id, Long customerId);
    List<BillingKey> findByCustomerId(Long customerId);

    // 소유 스코프(개인/조직) 기준 조회
    List<BillingKey> findByOwnerTypeAndOwnerIdAndStatusOrderByCreatedAtAsc(
            String ownerType, Long ownerId, String status);
    Optional<BillingKey> findByIdAndOwnerTypeAndOwnerId(Long id, String ownerType, Long ownerId);
}
