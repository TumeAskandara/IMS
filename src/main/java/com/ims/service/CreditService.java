package com.ims.service;

import com.ims.dto.request.CreditAccountRequest;
import com.ims.entity.CreditAccount;
import com.ims.exception.BadRequestException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.CreditAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditAccountRepository creditAccountRepository;

    @Transactional(readOnly = true)
    public Page<CreditAccount> getAllCreditAccounts(Pageable pageable) {
        return creditAccountRepository.findByIsDeletedFalse(pageable);
    }

    @Transactional(readOnly = true)
    public CreditAccount getCreditAccountById(Long id) {
        return creditAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CreditAccount", "id", id));
    }

    @Transactional
    public CreditAccount createCreditAccount(CreditAccountRequest request) {
        String accountNumber = "CA-" + System.currentTimeMillis();

        CreditAccount creditAccount = CreditAccount.builder()
                .accountNumber(accountNumber)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .customerAddress(request.getCustomerAddress())
                .idNumber(request.getIdNumber())
                .creditLimit(request.getCreditLimit())
                .totalCreditUsed(java.math.BigDecimal.ZERO)
                .isBlacklisted(false)
                .notes(request.getNotes())
                .build();

        return creditAccountRepository.save(creditAccount);
    }

    @Transactional
    public CreditAccount updateCreditAccount(Long id, CreditAccountRequest request) {
        CreditAccount creditAccount = getCreditAccountById(id);

        creditAccount.setCustomerName(request.getCustomerName());
        creditAccount.setCustomerPhone(request.getCustomerPhone());
        creditAccount.setCustomerEmail(request.getCustomerEmail());
        creditAccount.setCustomerAddress(request.getCustomerAddress());
        creditAccount.setIdNumber(request.getIdNumber());
        creditAccount.setCreditLimit(request.getCreditLimit());
        creditAccount.setNotes(request.getNotes());

        return creditAccountRepository.save(creditAccount);
    }

    @Transactional
    public CreditAccount blacklistAccount(Long id, String reason) {
        CreditAccount creditAccount = getCreditAccountById(id);
        creditAccount.setIsBlacklisted(true);
        creditAccount.setBlacklistReason(reason);
        return creditAccountRepository.save(creditAccount);
    }

    @Transactional
    public CreditAccount unblacklistAccount(Long id) {
        CreditAccount creditAccount = getCreditAccountById(id);
        creditAccount.setIsBlacklisted(false);
        creditAccount.setBlacklistReason(null);
        return creditAccountRepository.save(creditAccount);
    }

    @Transactional
    public void deleteCreditAccount(Long id) {
        CreditAccount creditAccount = getCreditAccountById(id);
        creditAccount.setIsDeleted(true);
        creditAccountRepository.save(creditAccount);
    }
}
