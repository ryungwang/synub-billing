package io.synub.billing.web;

import io.synub.billing.dto.Dtos.*;
import io.synub.billing.service.EntitlementService;
import io.synub.billing.service.IdempotencyService;
import io.synub.billing.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService service;
    private final IdempotencyService idempotency;
    private final EntitlementService entitlements;

    public SubscriptionController(SubscriptionService service, IdempotencyService idempotency,
                                  EntitlementService entitlements) {
        this.service = service;
        this.idempotency = idempotency;
        this.entitlements = entitlements;
    }

    @GetMapping
    public List<SubscriptionDto> list() {
        return service.list();
    }

    /** 전 스코프(개인 + 소속 조직 전체) 이용 중 구독 — 제품 둘러보기 배지용. 현재 컨텍스트와 무관. */
    @GetMapping("/mine")
    public List<MySubscriptionDto> mine() {
        return entitlements.mine();
    }

    /** 구독 생성(+첫 결제). Idempotency-Key 헤더로 이중 청구 방지. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionDto create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateSubscriptionRequest req) {
        return idempotency.execute("subscription:create", idempotencyKey,
                SubscriptionDto.class, () -> service.create(req));
    }

    @PostMapping("/{id}/cancel")
    public SubscriptionDto cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    /** 해지 철회 — 예약된 해지를 되돌린다(기간 만료 전). */
    @PostMapping("/{id}/resume")
    public SubscriptionDto resume(@PathVariable Long id) {
        return service.resume(id);
    }

    @PostMapping("/{id}/change-plan")
    public SubscriptionDto changePlan(@PathVariable Long id,
                                      @Valid @RequestBody ChangePlanRequest req) {
        return service.changePlan(id, req);
    }

    @PostMapping("/{id}/seats")
    public SubscriptionDto changeSeats(@PathVariable Long id,
                                       @Valid @RequestBody ChangeSeatsRequest req) {
        return service.changeSeats(id, req.seats());
    }
}
