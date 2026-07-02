package io.synub.billing.repo;

import io.synub.billing.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    /** 제품 내 최고가 플랜(개발사 무상 구독 = 최고 플랜). */
    Optional<Plan> findFirstByProductIdOrderByAmountDesc(Long productId);
}
