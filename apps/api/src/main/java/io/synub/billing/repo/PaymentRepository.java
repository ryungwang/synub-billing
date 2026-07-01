package io.synub.billing.repo;

import io.synub.billing.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPgPaymentId(String pgPaymentId);

    // 관리자 콘솔
    List<Payment> findTop100ByOrderByIdDesc();

    @Query("select coalesce(sum(p.amount),0) from Payment p where p.status = 'paid' and p.paidAt >= :from")
    long sumPaidSince(@Param("from") java.time.Instant from);

    /** 실제 결제 시각(paid_at, 없으면 created_at) 기준 최신순. */
    @Query("""
        select p from Payment p
        where p.subscription.customer.id = :customerId
        order by coalesce(p.paidAt, p.createdAt) desc
        """)
    List<Payment> findByCustomerOrderByEffectiveDateDesc(@Param("customerId") Long customerId);

    /** 소유 스코프(개인/조직) 구독의 결제 이력. */
    @Query("""
        select p from Payment p
        where p.subscription.ownerType = :ownerType and p.subscription.ownerId = :ownerId
        order by coalesce(p.paidAt, p.createdAt) desc
        """)
    List<Payment> findByOwnerOrderByEffectiveDateDesc(
            @Param("ownerType") String ownerType, @Param("ownerId") Long ownerId);
}
