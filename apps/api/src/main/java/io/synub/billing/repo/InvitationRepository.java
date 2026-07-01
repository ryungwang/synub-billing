package io.synub.billing.repo;

import io.synub.billing.domain.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    List<Invitation> findByOrganizationIdAndStatus(Long organizationId, String status);
    List<Invitation> findByEmailAndStatus(String email, String status);
    Optional<Invitation> findByOrganizationIdAndEmailAndStatus(Long organizationId, String email, String status);
}
