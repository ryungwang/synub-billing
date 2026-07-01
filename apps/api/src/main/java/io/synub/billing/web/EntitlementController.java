package io.synub.billing.web;

import io.synub.billing.dto.Dtos.EntitlementDto;
import io.synub.billing.service.EntitlementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EntitlementController {

    private final EntitlementService service;

    public EntitlementController(EntitlementService service) {
        this.service = service;
    }

    @GetMapping("/api/entitlements")
    public EntitlementDto entitlements(
            @RequestParam(name = "customer", required = false) String customer,
            @RequestParam(name = "service") String service) {
        return this.service.check(customer, service);
    }
}
