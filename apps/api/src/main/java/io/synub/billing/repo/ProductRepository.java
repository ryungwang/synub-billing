package io.synub.billing.repo;

import io.synub.billing.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStatusOrderBySortOrderAsc(String status);

    List<Product> findByStatusInOrderBySortOrderAsc(java.util.Collection<String> statuses);
    Optional<Product> findByServiceCode(String serviceCode);

    /** 관리자 콘솔 — 숨김 포함 전체(정렬순). */
    List<Product> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByServiceCode(String serviceCode);
}
