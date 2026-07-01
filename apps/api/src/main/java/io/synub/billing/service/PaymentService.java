package io.synub.billing.service;

import io.synub.billing.domain.Customer;
import io.synub.billing.dto.Dtos.PaymentDto;
import io.synub.billing.repo.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final CurrentUser currentUser;
    private final CurrentScope scope;
    private final DtoMapper mapper;

    public PaymentService(PaymentRepository payments, CurrentUser currentUser,
                          CurrentScope scope, DtoMapper mapper) {
        this.payments = payments;
        this.currentUser = currentUser;
        this.scope = scope;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> list() {
        if (scope.enforceOrgContext() != null) return List.of();
        Customer me = currentUser.resolve();
        return payments.findByCustomerOrderByEffectiveDateDesc(me.getId())
                .stream().map(mapper::toPayment).toList();
    }
}
