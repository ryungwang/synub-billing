package io.synub.billing.repo;

import io.synub.billing.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByExternalId(String externalId);
    List<Customer> findAllByOrderByIdDesc();
}
