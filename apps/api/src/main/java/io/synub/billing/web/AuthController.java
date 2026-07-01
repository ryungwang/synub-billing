package io.synub.billing.web;

import io.synub.billing.auth.AuthContext;
import io.synub.billing.auth.Identity;
import io.synub.billing.auth.IdentityContext;
import io.synub.billing.service.OrganizationService;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 현재 신원 확인용. 프론트가 "누구로/어떤 컨텍스트로 로그인됐는지 + 속한 조직"을 조회(컨텍스트 전환기용). */
@RestController
public class AuthController {

    private final OrganizationService organizations;

    public AuthController(OrganizationService organizations) {
        this.organizations = organizations;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        Identity id = IdentityContext.current();
        if (id == null) {
            // 필터가 보호 경로에서 이미 401 처리하므로 여기 도달 = 신원 미확립(구성 이상)
            throw new NotFoundException("현재 신원을 확인할 수 없습니다.");
        }
        AuthContext ctx = id.context();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("externalId", id.externalId());
        body.put("email", id.email());
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("type", ctx.type().name().toLowerCase());
        context.put("orgId", ctx.orgId());
        body.put("context", context);
        body.put("organizations", organizations.myOrganizations());
        return body;
    }
}
