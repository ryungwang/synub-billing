package io.synub.billing.web;

import io.synub.billing.auth.ServiceAuth;
import io.synub.billing.dto.Dtos.OrgMemberDto;
import io.synub.billing.dto.Dtos.ProvisionMemberRequest;
import io.synub.billing.service.OrgMemberProvisioningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 제품→빌링 조직 멤버 프로비저닝(그룹웨어 직원 추가). 사용자 토큰이 아닌 서비스 키(X-Service-Key)로 인증.
 * 공개경로(/api/orgs)이며 컨트롤러에서 서비스 키 자체 검증.
 */
@RestController
@RequestMapping("/api/orgs")
public class OrgMemberController {

    private final OrgMemberProvisioningService service;
    private final ServiceAuth serviceAuth;

    public OrgMemberController(OrgMemberProvisioningService service, ServiceAuth serviceAuth) {
        this.service = service;
        this.serviceAuth = serviceAuth;
    }

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    public OrgMemberDto addMember(
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey,
            @Valid @RequestBody ProvisionMemberRequest req) {
        serviceAuth.requireServiceKey(serviceKey);
        return service.addMember(req);
    }
}
