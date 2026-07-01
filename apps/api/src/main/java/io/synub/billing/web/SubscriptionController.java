package io.synub.billing.web;

import io.synub.billing.dto.Dtos.*;
import io.synub.billing.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    @GetMapping
    public List<SubscriptionDto> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionDto create(@Valid @RequestBody CreateSubscriptionRequest req) {
        return service.create(req);
    }

    @PostMapping("/{id}/cancel")
    public SubscriptionDto cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @PostMapping("/{id}/change-plan")
    public SubscriptionDto changePlan(@PathVariable Long id,
                                      @Valid @RequestBody ChangePlanRequest req) {
        return service.changePlan(id, req);
    }
}
