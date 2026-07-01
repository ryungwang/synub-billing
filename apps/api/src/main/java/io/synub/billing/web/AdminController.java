package io.synub.billing.web;

import io.synub.billing.dto.Dtos.AdminPaymentDto;
import io.synub.billing.dto.Dtos.AdminStatsDto;
import io.synub.billing.dto.Dtos.AdminSubscriptionDto;
import io.synub.billing.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 플랫폼 관리자 콘솔 API. 모든 엔드포인트는 admin claim 필요(서비스에서 인가). */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/stats")
    public AdminStatsDto stats() {
        return admin.stats();
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
}
