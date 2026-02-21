package com.ims.repository;

import com.ims.entity.Customer;
import com.ims.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    Optional<Customer> findByCustomerId(String customerId);
    
    Page<Customer> findByBranchId(Long branchId, Pageable pageable);
    
    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);
    
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.phone) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.customerId) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Customer> searchCustomers(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT c FROM Customer c ORDER BY c.lifetimeValue DESC")
    List<Customer> findTopCustomers(Pageable pageable);
    
    @Query("SELECT c FROM Customer c WHERE c.currentDebt > 0 ORDER BY c.currentDebt DESC")
    Page<Customer> findCustomersWithDebt(Pageable pageable);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhone(String phone);
    
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.branch.id = :branchId AND c.status = :status")
    Long countByBranchAndStatus(@Param("branchId") Long branchId, @Param("status") CustomerStatus status);
}
