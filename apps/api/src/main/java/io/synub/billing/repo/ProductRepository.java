package io.synub.billing.repo;

import io.synub.billing.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCompanyIdAndStatusOrderBySortOrderAsc(Long companyId, String status);
    Optional<Product> findByCompanyIdAndServiceCode(Long companyId, String serviceCode);
}
