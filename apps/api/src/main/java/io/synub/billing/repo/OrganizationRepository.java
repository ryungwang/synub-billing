package io.synub.billing.repo;

import io.synub.billing.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    boolean existsByBusinessNo(String businessNo);
    List<Organization> findAllByOrderByIdDesc();
}
