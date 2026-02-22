package com.ims.service;

import com.ims.dto.request.DebtPaymentRequest;
import com.ims.entity.*;
import com.ims.enums.DebtStatus;
import com.ims.enums.NotificationPriority;
import com.ims.enums.NotificationType;
import com.ims.enums.Role;
import com.ims.exception.BadRequestException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final DebtPaymentRepository debtPaymentRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<Debt> getAllDebts(Pageable pageable) {
        return debtRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Debt getDebtById(Long id) {
        return debtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Debt", "id", id));
    }

    @Transactional(readOnly = true)
    public List<Debt> getOverdueDebts() {
        return debtRepository.findOverdueDebts(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Page<Debt> getDebtsByStatus(DebtStatus status, Pageable pageable) {
        return debtRepository.findByStatusAndIsDeletedFalse(status, pageable);
    }

    @Transactional
    public DebtPayment recordPayment(Long debtId, DebtPaymentRequest request) {
        Debt debt = getDebtById(debtId);

        if (request.getAmount().compareTo(debt.getBalanceDue()) > 0) {
            throw new BadRequestException("Payment amount exceeds balance due");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User receivedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        DebtPayment payment = DebtPayment.builder()
                .debt(debt)
                .paymentDate(LocalDateTime.now())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .referenceNumber(request.getReferenceNumber())
                .receivedBy(receivedBy)
                .notes(request.getNotes())
                .build();

        debt.setAmountPaid(debt.getAmountPaid().add(request.getAmount()));
        debt.setBalanceDue(debt.getBalanceDue().subtract(request.getAmount()));

        // Update debt status
        if (debt.getBalanceDue().compareTo(BigDecimal.ZERO) == 0) {
            debt.setStatus(DebtStatus.FULLY_PAID);
        } else if (debt.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            debt.setStatus(DebtStatus.PARTIALLY_PAID);
        }

        debtRepository.save(debt);

        // Update credit account
        CreditAccount creditAccount = debt.getCreditAccount();
        if (creditAccount != null) {
            creditAccount.setTotalCreditUsed(
                    creditAccount.getTotalCreditUsed().subtract(request.getAmount()));
            creditAccountRepository.save(creditAccount);
        }

        // Update Customer.currentDebt so credit limits stay accurate
        Customer customer = debt.getSale().getCustomer();
        if (customer != null) {
            customer.reduceDebt(request.getAmount());
            customerRepository.save(customer);
        }

        // Notify managers that payment was received
        List<User> managers = userRepository.findByRole(Role.MANAGER);
        for (User manager : managers) {
            notificationService.createNotification(
                    manager.getId(),
                    NotificationType.USER_ACTION,
                    NotificationPriority.LOW,
                    "Payment Received",
                    String.format("Payment of $%.2f received from customer (Sale #%s)",
                            request.getAmount(),
                            debt.getSale().getInvoiceNumber())
            );
        }

        return debtPaymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDebtSummary() {
        Map<String, Object> summary = new HashMap<>();

        BigDecimal totalOutstanding = debtRepository.getTotalOutstandingDebt();
        BigDecimal totalOverdue = debtRepository.getTotalOverdueDebt();
        Long activeCount = debtRepository.getActiveDebtsCount();

        summary.put("totalOutstanding", totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO);
        summary.put("totalOverdue", totalOverdue != null ? totalOverdue : BigDecimal.ZERO);
        summary.put("activeDebtsCount", activeCount != null ? activeCount : 0);

        return summary;
    }

    // Run this daily to check overdue debts at 9 AM
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkAndUpdateOverdueDebts() {
        // Targeted query: only fetches debts that are past due and not yet marked OVERDUE/FULLY_PAID
        List<Debt> newlyOverdueDebts = debtRepository.findNewlyOverdueDebts(LocalDate.now());

        if (newlyOverdueDebts.isEmpty()) {
            return;
        }

        // Get all managers once
        List<User> managers = userRepository.findByRole(Role.MANAGER);

        for (Debt debt : newlyOverdueDebts) {
            debt.setStatus(DebtStatus.OVERDUE);
            debtRepository.save(debt);

            // Notify managers
            for (User manager : managers) {
                notificationService.createNotification(
                        manager.getId(),
                        NotificationType.OVERDUE_DEBT,
                        NotificationPriority.CRITICAL,
                        "Overdue Payment Alert",
                        String.format("Sale #%s has overdue payment of $%.2f due on %s",
                                debt.getSale().getInvoiceNumber(),
                                debt.getBalanceDue(),
                                debt.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                );
            }
        }
    }
}