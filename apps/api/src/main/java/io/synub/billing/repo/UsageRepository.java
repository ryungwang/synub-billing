package io.synub.billing.repo;

import io.synub.billing.domain.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsageRepository extends JpaRepository<UsageRecord, Long> {
    Optional<UsageRecord> findByExternalIdAndServiceCode(String externalId, String serviceCode);
}
