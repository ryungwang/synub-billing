package io.synub.billing.repo;

import io.synub.billing.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCustomerIdOrderByCreatedAtAsc(Long customerId);
    Optional<Subscription> findByIdAndCustomerId(Long id, Long customerId);
    long countByBillingKeyIdAndStatusNot(Long billingKeyId, String status);
    long countByPlanProductIdAndStatus(Long productId, String status);

    @Query("""
        select s from Subscription s
        where s.status in :statuses and s.nextBillingDate <= :date
        order by s.nextBillingDate asc
        """)
    List<Subscription> findDue(
            @Param("statuses") Collection<String> statuses,
            @Param("date") LocalDate date);

    @Query("""
        select s from Subscription s
        where s.customer.id = :customerId
          and s.plan.product.serviceCode = :serviceCode
        order by s.createdAt desc
        """)
    List<Subscription> findByCustomerAndService(
            @Param("customerId") Long customerId,
            @Param("serviceCode") String serviceCode);
}
