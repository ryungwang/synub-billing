package io.synub.billing.web;

import io.synub.billing.dto.Dtos.CardDto;
import io.synub.billing.dto.Dtos.RegisterBillingKeyRequest;
import io.synub.billing.service.BillingKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/billing/keys")
public class BillingKeyController {

    private final BillingKeyService service;

    public BillingKeyController(BillingKeyService service) {
        this.service = service;
    }

    @GetMapping
    public List<CardDto> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardDto register(@Valid @RequestBody RegisterBillingKeyRequest req) {
        return service.register(req);
    }

    @PostMapping("/{id}/primary")
    public void setPrimary(@PathVariable Long id) {
        service.setPrimary(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
