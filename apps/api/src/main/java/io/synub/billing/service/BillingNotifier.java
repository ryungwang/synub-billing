package io.synub.billing.service;

import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Membership;
import io.synub.billing.domain.Subscription;
import io.synub.billing.mail.BillingMailer;
import io.synub.billing.repo.CustomerRepository;
import io.synub.billing.repo.MembershipRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 결제 상태 알림 대상 해석 + 발송. 수신자:
 * 개인 구독 → 본인, 조직 구독 → 결제 관리 권한 멤버(owner/billing_manager) 전원.
 */
@Service
public class BillingNotifier {

    private final MembershipRepository memberships;
    private final CustomerRepository customers;
    private final BillingMailer mailer;

    public BillingNotifier(MembershipRepository memberships, CustomerRepository customers,
                           BillingMailer mailer) {
        this.memberships = memberships;
        this.customers = customers;
        this.mailer = mailer;
    }

    public void paymentFailed(Subscription sub) {
        String product = product(sub);
        for (String to : recipients(sub)) {
            mailer.paymentFailed(to, product, sub.chargeAmount(), sub.getNextBillingDate());
        }
    }

    public void suspended(Subscription sub) {
        String product = product(sub);
        for (String to : recipients(sub)) {
            mailer.suspended(to, product);
        }
    }

    public void recovered(Subscription sub) {
        String product = product(sub);
        for (String to : recipients(sub)) {
            mailer.recovered(to, product);
        }
    }

    private String product(Subscription sub) {
        return sub.getPlan().getProduct().getName();
    }

    /** 알림 수신 이메일 목록. */
    private List<String> recipients(Subscription sub) {
        if (Owner.ORGANIZATION.equals(sub.getOwnerType())) {
            return memberships.findByOrganizationId(sub.getOwnerId()).stream()
                    .filter(Membership::canManageBilling)
                    .map(m -> customers.findById(m.getCustomerId())
                            .map(Customer::getEmail).orElse(null))
                    .filter(e -> e != null && !e.isBlank())
                    .distinct()
                    .toList();
        }
        String email = sub.getCustomer().getEmail();
        return (email != null && !email.isBlank()) ? List.of(email) : List.of();
    }
}
