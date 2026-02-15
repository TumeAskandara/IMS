package com.ims.repository;

import com.ims.entity.DebtPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DebtPaymentRepository extends JpaRepository<DebtPayment, Long> {
    
    List<DebtPayment> findByDebtId(Long debtId);
    
    List<DebtPayment> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end);
    
    List<DebtPayment> findByReceivedById(Long userId);
}
