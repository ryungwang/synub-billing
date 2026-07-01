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
        return products.findByStatusOrderBySortOrderAsc("active")
                .stream()
                .map(p -> mapper.toProduct(
                        p, subscriptions.countByPlanProductIdAndStatus(p.getId(), "active")))
                .toList();
    }
}
