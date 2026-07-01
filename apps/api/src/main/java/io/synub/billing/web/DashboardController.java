package io.synub.billing.web;

import io.synub.billing.dto.Dtos.DashboardDto;
import io.synub.billing.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        return service.get();
    }
}
