package io.synub.billing.web;

import io.synub.billing.auth.ServiceAuth;
import io.synub.billing.dto.Dtos.ContextsDto;
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

    /**
     * entitlement 판정. {@code context}로 컨텍스트 스코프를 지정한다.
     *   미지정 → 개인 + 모든 조직(하위호환) / "personal" → 개인만 / "org:{orgCode}" → 해당 조직만.
     */
    @GetMapping("/api/entitlements")
    public EntitlementDto entitlements(
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey,
            @RequestParam(name = "customer", required = false) String customer,
            @RequestParam(name = "service") String service,
            @RequestParam(name = "context", required = false) String context) {
        serviceAuth.requireServiceKey(serviceKey);
        return this.service.check(customer, service, context);
    }

    /**
     * 사용자의 컨텍스트 목록(개인 + 소속 조직). 제품이 컨텍스트 스위처를 그리는 데 쓴다.
     * 각 항목의 {@code context} 값을 그대로 {@code /api/entitlements}의 context 파라미터로 넘기면 된다.
     */
    @GetMapping("/api/contexts")
    public ContextsDto contexts(
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey,
            @RequestParam(name = "customer", required = false) String customer) {
        serviceAuth.requireServiceKey(serviceKey);
        return this.service.listContexts(customer);
    }
}
