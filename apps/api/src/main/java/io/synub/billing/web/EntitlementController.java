package io.synub.billing.web;

import io.synub.billing.auth.ServiceAuth;
import io.synub.billing.dto.Dtos.EntitlementDto;
import io.synub.billing.service.EntitlementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 제품→빌링 권한(entitlement) 조회. 공개경로(사용자 토큰 없음)이므로
 * 임의 고객 정보 조회를 막기 위해 서비스 키(X-Service-Key)로 인증한다.
 */
@RestController
public class EntitlementController {

    private final EntitlementService service;
    private final ServiceAuth serviceAuth;

    public EntitlementController(EntitlementService service, ServiceAuth serviceAuth) {
        this.service = service;
        this.serviceAuth = serviceAuth;
    }

    @GetMapping("/api/entitlements")
    public EntitlementDto entitlements(
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey,
            @RequestParam(name = "customer", required = false) String customer,
            @RequestParam(name = "service") String service) {
        serviceAuth.requireServiceKey(serviceKey);
        return this.service.check(customer, service);
    }
}
