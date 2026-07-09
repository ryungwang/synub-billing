package io.synub.billing.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plan.amountForSeats 의 정수 오버플로 가드(C2). */
class PlanTest {

    private Plan perSeat(int unitAmount) {
        Plan p = new Plan();
        ReflectionTestUtils.setField(p, "amount", unitAmount);
        ReflectionTestUtils.setField(p, "pricingType", "per_seat");
        return p;
    }

    @Test
    void 정상_좌석_수는_곱셈_결과를_그대로_반환() {
        assertThat(perSeat(9_900).amountForSeats(100)).isEqualTo(990_000);
    }

    @Test
    void 정액_플랜은_좌석_수를_무시() {
        Plan flat = new Plan();
        ReflectionTestUtils.setField(flat, "amount", 50_000);
        ReflectionTestUtils.setField(flat, "pricingType", "flat");
        assertThat(flat.amountForSeats(999)).isEqualTo(50_000);
    }

    @Test
    void 오버플로를_유발하는_좌석_수는_조용히_wrap하지_않고_예외() {
        // 3,000,000 * 999 = 2,997,000,000 > Integer.MAX_VALUE — 예전엔 음수로 wrap돼 과소결제됐다.
        assertThatThrownBy(() -> perSeat(3_000_000).amountForSeats(999))
                .isInstanceOf(IllegalStateException.class);
    }
}
