package com.rikkeibank.customer.repository;

import com.rikkeibank.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerCif(String customerCif);
    boolean existsByCustomerCif(String customerCif);
    boolean existsByIdentityCard(String identityCard);
}
