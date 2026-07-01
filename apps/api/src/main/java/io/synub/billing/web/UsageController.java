package io.synub.billing.web;

import io.synub.billing.auth.ServiceAuth;
import io.synub.billing.domain.UsageRecord;
import io.synub.billing.dto.Dtos.ReportUsageRequest;
import io.synub.billing.repo.UsageRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * 제품→빌링 사용량 보고. 공개경로(사용자 토큰 없음)이므로 서비스 키(X-Service-Key)로 인증.
 * (external_id, service_code) upsert — 대시보드·entitlements의 사용량으로 표시된다.
 */
@RestController
public class UsageController {

    private final UsageRepository usage;
    private final ServiceAuth serviceAuth;

    public UsageController(UsageRepository usage, ServiceAuth serviceAuth) {
        this.usage = usage;
        this.serviceAuth = serviceAuth;
    }

    @PostMapping("/api/usage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void report(
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey,
            @Valid @RequestBody ReportUsageRequest req) {
        serviceAuth.requireServiceKey(serviceKey);
        UsageRecord rec = usage.findByExternalIdAndServiceCode(req.customer(), req.service())
                .orElseGet(() -> new UsageRecord(req.customer(), req.service()));
        rec.report(
                req.label() != null ? req.label() : "사용량",
                req.unit() != null ? req.unit() : "건",
                Math.max(0, req.used()), req.limit());
        usage.save(rec);
    }
}
