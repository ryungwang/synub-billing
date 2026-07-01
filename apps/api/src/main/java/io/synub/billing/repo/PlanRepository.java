package io.synub.billing.repo;

import io.synub.billing.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findByIdAndProductCompanyId(Long id, Long companyId);
}
