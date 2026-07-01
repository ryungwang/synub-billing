package io.synub.billing.repo;

import io.synub.billing.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByScopeAndIdemKey(String scope, String idemKey);

    /** 오래된 멱등키 정리. */
    @Modifying
    @Query("delete from IdempotencyKey k where k.createdAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
