package io.synub.billing.web;

import io.synub.billing.dto.Dtos.PaymentDto;
import io.synub.billing.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/payments")
    public List<PaymentDto> list() {
        return service.list();
    }
}
