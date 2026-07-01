package io.synub.billing.repo;

import io.synub.billing.domain.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByCustomerId(Long customerId);
    Optional<Membership> findByOrganizationIdAndCustomerId(Long organizationId, Long customerId);
    List<Membership> findByOrganizationId(Long organizationId);
}
