package io.synub.billing.service;

import io.synub.billing.dto.Dtos.ProductDto;
import io.synub.billing.repo.ProductRepository;
import io.synub.billing.repo.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final ProductRepository products;
    private final SubscriptionRepository subscriptions;
    private final DtoMapper mapper;

    public CatalogService(ProductRepository products, SubscriptionRepository subscriptions,
                          DtoMapper mapper) {
        this.products = products;
        this.subscriptions = subscriptions;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> listProducts() {
        // active(구독가능) + coming_soon(준비중·티저 노출)만 카탈로그에 노출. inactive(숨김)는 제외.
        // 준비중 제품은 status로 프론트가 배지·구독불가 처리(구독 자체는 서버에서도 차단).
        return products.findByStatusInOrderBySortOrderAsc(List.of("active", "coming_soon"))
                .stream()
                .map(p -> mapper.toProduct(
                        p, subscriptions.countByPlanProductIdAndStatus(p.getId(), "active")))
                .toList();
    }
}
