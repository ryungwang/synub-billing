package io.synub.billing.service;

import io.synub.billing.domain.BillingKey;
import io.synub.billing.domain.Customer;
import io.synub.billing.dto.Dtos.CardDto;
import io.synub.billing.dto.Dtos.RegisterBillingKeyRequest;
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

    public BillingKeyService(BillingKeyRepository keys, SubscriptionRepository subscriptions,
                             CurrentUser currentUser, CurrentScope scope, DtoMapper mapper) {
        this.keys = keys;
        this.subscriptions = subscriptions;
        this.currentUser = currentUser;
        this.scope = scope;
        this.mapper = mapper;
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
        List<BillingKey> existing = keys.findByOwnerTypeAndOwnerIdAndStatusOrderByCreatedAtAsc(
                owner.type(), owner.id(), "active");
        boolean makePrimary = Boolean.TRUE.equals(req.primary()) || existing.isEmpty();
        if (makePrimary) {
            existing.forEach(k -> k.setPrimary(false));
        }
        BillingKey key = new BillingKey(me, req.pgBillingKey(), req.cardCompany(),
                req.cardLast4(), req.cardType(), makePrimary);
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
