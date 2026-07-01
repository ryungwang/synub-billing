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

    // 관리자 콘솔
    List<Subscription> findAllByOrderByIdDesc();
    List<Subscription> findByStatus(String status);
    long countByStatus(String status);

    // 소유 스코프(개인/조직) 기준 조회
    List<Subscription> findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(String ownerType, Long ownerId);
    Optional<Subscription> findByIdAndOwnerTypeAndOwnerId(Long id, String ownerType, Long ownerId);

    @Query("""
        select s from Subscription s
        where s.ownerType = :ownerType and s.ownerId = :ownerId
          and s.plan.product.serviceCode = :serviceCode
        order by s.createdAt desc
        """)
    List<Subscription> findByOwnerAndService(
            @Param("ownerType") String ownerType,
            @Param("ownerId") Long ownerId,
            @Param("serviceCode") String serviceCode);

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
