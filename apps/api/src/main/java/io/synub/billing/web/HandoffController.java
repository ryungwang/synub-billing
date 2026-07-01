package io.synub.billing.web;

import io.synub.billing.config.AppProperties;
import io.synub.billing.domain.Organization;
import io.synub.billing.domain.Product;
import io.synub.billing.dto.Dtos.HandoffDto;
import io.synub.billing.repo.ProductRepository;
import io.synub.billing.service.CurrentUser;
import io.synub.billing.service.OrganizationService;
import io.synub.billing.web.ApiExceptions.BadRequestException;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * 제품 초기설정 온보딩 핸드오프 — 조직이 셋업 필요 제품(그룹웨어 등)을 구독한 뒤 그 제품 온보딩 페이지로
 * 이동할 서명 링크를 발급. **범용**: 특정 제품에 종속되지 않고 service_code + product.onboarding_url로 동작.
 * 서명 규약은 웹훅과 동일(sha256=hex, app.webhook.secret). 페이로드=customer|orgCode(변조 방지).
 */
@RestController
@RequestMapping("/organizations")
public class HandoffController {

    private final OrganizationService organizations;
    private final ProductRepository products;
    private final CurrentUser currentUser;
    private final AppProperties props;

    public HandoffController(OrganizationService organizations, ProductRepository products,
                             CurrentUser currentUser, AppProperties props) {
        this.organizations = organizations;
        this.products = products;
        this.currentUser = currentUser;
        this.props = props;
    }

    @GetMapping("/{orgId}/handoff")
    public HandoffDto handoff(@PathVariable Long orgId, @RequestParam("service") String service) {
        organizations.requireManager(orgId); // owner/billing_manager만
        Organization org = organizations.org(orgId);
        if (!org.isVerified() || org.getOrgCode() == null) {
            throw new BadRequestException("인증 완료된 조직만 초기설정할 수 있습니다.");
        }
        Product product = products.findByServiceCode(service)
                .orElseThrow(() -> new NotFoundException("제품을 찾을 수 없습니다: " + service));
        if (product.getOnboardingUrl() == null || product.getOnboardingUrl().isBlank()) {
            throw new BadRequestException("이 제품은 초기설정 온보딩을 제공하지 않습니다.");
        }
        String customer = currentUser.externalId();
        String orgCode = org.getOrgCode();
        String sig = "sha256=" + hmacHex(customer + "|" + orgCode, props.webhook().secret());
        String url = product.getOnboardingUrl()
                + "?customer=" + enc(customer)
                + "&company=" + enc(org.getName())
                + "&orgCode=" + enc(orgCode)
                + "&sig=" + enc(sig);
        return new HandoffDto(url);
    }

    private static String hmacHex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("핸드오프 서명 실패", e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
