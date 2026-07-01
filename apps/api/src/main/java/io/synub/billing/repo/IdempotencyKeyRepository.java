package io.synub.billing.repo;

import io.synub.billing.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByScopeAndIdemKey(String scope, String idemKey);
}
