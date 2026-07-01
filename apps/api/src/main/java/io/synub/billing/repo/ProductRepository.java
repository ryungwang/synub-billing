package io.synub.billing.repo;

import io.synub.billing.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStatusOrderBySortOrderAsc(String status);
    Optional<Product> findByServiceCode(String serviceCode);
}
