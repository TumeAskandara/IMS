package com.ims.service;

import com.ims.entity.Currency;
import com.ims.exception.BadRequestException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    public Currency createCurrency(String code, String name, String symbol, BigDecimal exchangeRate, boolean isBase) {
        if (currencyRepository.existsByCode(code.toUpperCase())) {
            throw new BadRequestException("Currency with code " + code + " already exists");
        }

        if (isBase) {
            currencyRepository.findByIsBaseTrue().ifPresent(existing -> {
                existing.setIsBase(false);
                currencyRepository.save(existing);
            });
            exchangeRate = BigDecimal.ONE;
        }

        Currency currency = Currency.builder()
                .code(code.toUpperCase())
                .name(name)
                .symbol(symbol)
                .exchangeRate(exchangeRate)
                .isBase(isBase)
                .isActive(true)
                .lastUpdated(LocalDateTime.now())
                .build();

        currency = currencyRepository.save(currency);
        log.info("Created currency: {} ({})", code, name);
        return currency;
    }

    public Currency updateExchangeRate(String code, BigDecimal newRate) {
        Currency currency = currencyRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "code", code));

        if (currency.getIsBase()) {
            throw new BadRequestException("Cannot change exchange rate of base currency");
        }

        currency.setExchangeRate(newRate);
        currency.setLastUpdated(LocalDateTime.now());
        currency = currencyRepository.save(currency);
        log.info("Updated exchange rate for {}: {}", code, newRate);
        return currency;
    }

    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal amount, String fromCode, String toCode) {
        if (fromCode.equalsIgnoreCase(toCode)) {
            return amount;
        }

        Currency from = currencyRepository.findByCode(fromCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "code", fromCode));
        Currency to = currencyRepository.findByCode(toCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "code", toCode));

        BigDecimal baseAmount = amount.divide(from.getExchangeRate(), 10, RoundingMode.HALF_UP);
        return baseAmount.multiply(to.getExchangeRate()).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<Currency> getAllActiveCurrencies() {
        return currencyRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public Currency getCurrencyByCode(String code) {
        return currencyRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "code", code));
    }

    public Currency toggleActive(String code) {
        Currency currency = currencyRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "code", code));
        if (currency.getIsBase()) {
            throw new BadRequestException("Cannot deactivate base currency");
        }
        currency.setIsActive(!currency.getIsActive());
        return currencyRepository.save(currency);
    }
}