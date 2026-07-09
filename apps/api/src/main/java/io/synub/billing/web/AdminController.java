package io.synub.billing.web;

import io.synub.billing.dto.Dtos.AdminAnalyticsDto;
import io.synub.billing.dto.Dtos.AdminCustomerDto;
import io.synub.billing.dto.Dtos.AdminInquiryDto;
import io.synub.billing.dto.Dtos.AdminOrgDto;
import io.synub.billing.dto.Dtos.AdminPaymentDto;
import io.synub.billing.dto.Dtos.AdminStatsDto;
import io.synub.billing.dto.Dtos.AdminSubscriptionDto;
import io.synub.billing.dto.Dtos.ProductAdminDto;
import io.synub.billing.dto.Dtos.ProductMetaRequest;
import io.synub.billing.dto.Dtos.RejectOrgRequest;
import io.synub.billing.service.AdminService;
import io.synub.billing.service.InquiryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 플랫폼 관리자 콘솔 API. 모든 엔드포인트는 admin claim 필요(서비스에서 인가). */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService admin;
    private final InquiryService inquiries;

    public AdminController(AdminService admin, InquiryService inquiries) {
        this.admin = admin;
        this.inquiries = inquiries;
    }

    @GetMapping("/stats")
    public AdminStatsDto stats() {
        return admin.stats();
    }

    @GetMapping("/analytics")
    public AdminAnalyticsDto analytics() {
        return admin.analytics();
    }

    @GetMapping("/subscriptions")
    public List<AdminSubscriptionDto> subscriptions() {
        return admin.subscriptions();
    }

    @GetMapping("/payments")
    public List<AdminPaymentDto> payments() {
        return admin.payments();
    }

    @PostMapping("/payments/{id}/refund")
    public AdminPaymentDto refund(@PathVariable Long id) {
        return admin.refund(id);
    }

    // ---- 제품 메타 관리(가격/플랜은 마이그레이션 전용) ----
    @GetMapping("/products")
    public List<ProductAdminDto> products() {
        return admin.products();
    }

    @PostMapping("/products")
    public ProductAdminDto createProduct(@RequestBody ProductMetaRequest req) {
        return admin.createProduct(req);
    }

    @PutMapping("/products/{id}")
    public ProductAdminDto updateProduct(@PathVariable Long id, @RequestBody ProductMetaRequest req) {
        return admin.updateProduct(id, req);
    }

    // ---- 회사 인증 심사 ----

    @GetMapping("/customers")
    public List<AdminCustomerDto> customers() {
        return admin.customers();
    }

    @GetMapping("/organizations")
    public List<AdminOrgDto> organizations() {
        return admin.organizations();
    }

    @PostMapping("/organizations/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveOrg(@PathVariable Long id) {
        admin.approveOrganization(id);
    }

    @PostMapping("/organizations/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectOrg(@PathVariable Long id, @RequestBody(required = false) RejectOrgRequest req) {
        admin.rejectOrganization(id, req != null ? req.reason() : null);
    }

    @GetMapping("/organizations/{id}/document")
    public ResponseEntity<byte[]> orgDocument(@PathVariable Long id) {
        AdminService.DocumentContent doc = admin.organizationDocument(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                // 폴리글롯 업로드가 선언 타입과 다르게 스니핑돼 실행되는 것 차단(특히 inline 렌더).
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "inline; filename=\"business-doc\"")
                .body(doc.content());
    }

    // ---- 문의(contact form) ----

    @GetMapping("/inquiries")
    public List<AdminInquiryDto> inquiries() {
        return inquiries.list();
    }

    @PostMapping("/inquiries/{id}/resolve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolveInquiry(@PathVariable Long id) {
        inquiries.resolve(id);
    }

    @GetMapping("/inquiries/{id}/attachment")
    public ResponseEntity<byte[]> inquiryAttachment(@PathVariable Long id) {
        InquiryService.Attachment a = inquiries.attachment(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "attachment; filename=\"" + a.filename().replace("\"", "") + "\"")
                .body(a.content());
    }
}

