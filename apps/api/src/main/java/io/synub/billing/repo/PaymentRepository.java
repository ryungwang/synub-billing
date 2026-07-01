package io.synub.billing.repo;

import io.synub.billing.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPgPaymentId(String pgPaymentId);

    /** 실제 결제 시각(paid_at, 없으면 created_at) 기준 최신순. */
    @Query("""
        select p from Payment p
        where p.subscription.customer.id = :customerId
        order by coalesce(p.paidAt, p.createdAt) desc
        """)
    List<Payment> findByCustomerOrderByEffectiveDateDesc(@Param("customerId") Long customerId);
}
