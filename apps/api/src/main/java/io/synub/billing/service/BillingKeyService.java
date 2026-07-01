package io.synub.billing.service;

import io.synub.billing.domain.BillingKey;
import io.synub.billing.domain.Customer;
import io.synub.billing.dto.Dtos.CardDto;
import io.synub.billing.dto.Dtos.RegisterBillingKeyRequest;
import io.synub.billing.gateway.PaymentGateway;
import io.synub.billing.repo.BillingKeyRepository;
import io.synub.billing.repo.SubscriptionRepository;
import io.synub.billing.web.ApiExceptions.BadRequestException;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BillingKeyService {

    private final BillingKeyRepository keys;
    private final SubscriptionRepository subscriptions;
    private final CurrentUser currentUser;
    private final CurrentScope scope;
    private final DtoMapper mapper;
    private final PaymentGateway gateway;

    public BillingKeyService(BillingKeyRepository keys, SubscriptionRepository subscriptions,
                             CurrentUser currentUser, CurrentScope scope, DtoMapper mapper,
                             PaymentGateway gateway) {
        this.keys = keys;
        this.subscriptions = subscriptions;
        this.currentUser = currentUser;
        this.scope = scope;
        this.mapper = mapper;
        this.gateway = gateway;
    }

    @Transactional(readOnly = true)
    public List<CardDto> list() {
        Owner owner = scope.readOwner();
        return keys.findByOwnerTypeAndOwnerIdAndStatusOrderByCreatedAtAsc(owner.type(), owner.id(), "active")
                .stream()
                .map(k -> mapper.toCard(
                        k, subscriptions.countByBillingKeyIdAndStatusNot(k.getId(), "canceled")))
                .toList();
    }

    @Transactional
    public CardDto register(RegisterBillingKeyRequest req) {
        Customer me = currentUser.resolve();
        Owner owner = scope.writeOwner();
        // PG 청구에 쓸 전화번호 수집(빌링키 발급 시 입력받음)
        if (req.phone() != null && !req.phone().isBlank()) {
            me.setPhone(req.phone().replaceAll("[^0-9]", ""));
        }
        List<BillingKey> existing = keys.findByOwnerTypeAndOwnerIdAndStatusOrderByCreatedAtAsc(
                owner.type(), owner.id(), "active");
        boolean makePrimary = Boolean.TRUE.equals(req.primary()) || existing.isEmpty();
        if (makePrimary) {
            existing.forEach(k -> k.setPrimary(false));
        }
        // 카드 메타 미제공(실연동 발급) 시 PG 조회로 카드사·끝4자리 보강
        String cardCompany = req.cardCompany(), cardLast4 = req.cardLast4(), cardType = req.cardType();
        if (cardCompany == null || cardLast4 == null) {
            var info = gateway.lookupBillingKey(req.pgBillingKey());
            if (info.isPresent()) {
                if (cardCompany == null) cardCompany = info.get().cardCompany();
                if (cardLast4 == null) cardLast4 = info.get().cardLast4();
                if (cardType == null) cardType = info.get().cardType();
            }
        }
        BillingKey key = new BillingKey(me, req.pgBillingKey(), cardCompany,
                cardLast4, cardType, makePrimary);
        key.setOwner(owner.type(), owner.id());
        keys.save(key);
        return mapper.toCard(key, 0);
    }

    @Transactional
    public void setPrimary(Long id) {
        Owner owner = scope.writeOwner();
        BillingKey target = keys.findByIdAndOwnerTypeAndOwnerId(id, owner.type(), owner.id())
                .orElseThrow(() -> new NotFoundException("결제수단을 찾을 수 없습니다."));
        keys.findByOwnerTypeAndOwnerIdAndStatusOrderByCreatedAtAsc(owner.type(), owner.id(), "active")
                .forEach(k -> k.setPrimary(k.getId().equals(target.getId())));
    }

    @Transactional
    public void delete(Long id) {
        Owner owner = scope.writeOwner();
        BillingKey key = keys.findByIdAndOwnerTypeAndOwnerId(id, owner.type(), owner.id())
                .orElseThrow(() -> new NotFoundException("결제수단을 찾을 수 없습니다."));
        if (key.isPrimary()) {
            throw new BadRequestException("대표 결제수단은 삭제할 수 없습니다. 다른 카드를 대표로 지정하세요.");
        }
        long active = subscriptions.countByBillingKeyIdAndStatusNot(key.getId(), "canceled");
        if (active > 0) {
            throw new BadRequestException("이 카드로 청구 중인 구독이 있어 삭제할 수 없습니다.");
        }
        key.setStatus("deleted");
    }
}
