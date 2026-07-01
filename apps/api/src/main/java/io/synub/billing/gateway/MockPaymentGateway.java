package io.synub.billing.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 로컬/테스트용 목 게이트웨이 — app.portone.enabled=false(기본)일 때 활성.
 * 항상 성공 처리하되, "fail_" 로 시작하는 빌링키는 강제 실패(재시도/상태머신 검증용).
 */
@Component
@ConditionalOnProperty(name = "app.portone.enabled", havingValue = "false", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    private final AtomicLong seq = new AtomicLong(1000);

    @Override
    public ChargeResult charge(ChargeRequest req) {
        if (req.billingKey() != null && req.billingKey().startsWith("fail_")) {
            return ChargeResult.fail("테스트 강제 실패 (카드 거절)");
        }
        String id = "mock_pay_"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "_" + seq.incrementAndGet();
        return ChargeResult.ok(id);
    }
}
