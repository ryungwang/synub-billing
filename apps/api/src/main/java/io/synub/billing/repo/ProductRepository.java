package io.synub.billing.repo;

import io.synub.billing.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCompanyIdAndStatusOrderBySortOrderAsc(Long companyId, String status);
}
