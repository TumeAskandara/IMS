package com.ims.repository;

import com.ims.entity.CreditAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditAccountRepository extends JpaRepository<CreditAccount, Long> {
    
    Optional<CreditAccount> findByAccountNumber(String accountNumber);
    
    Page<CreditAccount> findByIsDeletedFalse(Pageable pageable);
    
    List<CreditAccount> findByIsBlacklistedTrueAndIsDeletedFalse();
    
    @Query("SELECT ca FROM CreditAccount ca WHERE ca.isDeleted = false AND " +
           "(LOWER(ca.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ca.customerPhone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ca.accountNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CreditAccount> searchCreditAccounts(String search, Pageable pageable);
}
