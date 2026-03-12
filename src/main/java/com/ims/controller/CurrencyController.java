package com.ims.controller;

import com.ims.dto.response.ApiResponse;
import com.ims.entity.Currency;
import com.ims.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
@Tag(name = "Multi-Currency", description = "Currency management and conversion")
@SecurityRequirement(name = "bearerAuth")
public class CurrencyController {

    private final CurrencyService currencyService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create currency")
    public ResponseEntity<ApiResponse<Currency>> createCurrency(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam(required = false) String symbol,
            @RequestParam BigDecimal exchangeRate,
            @RequestParam(defaultValue = "false") boolean isBase) {
        Currency currency = currencyService.createCurrency(code, name, symbol, exchangeRate, isBase);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Currency created", currency));
    }

    @PutMapping("/{code}/rate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update exchange rate")
    public ResponseEntity<ApiResponse<Currency>> updateRate(
            @PathVariable String code,
            @RequestParam BigDecimal rate) {
        Currency currency = currencyService.updateExchangeRate(code, rate);
        return ResponseEntity.ok(ApiResponse.success("Exchange rate updated", currency));
    }

    @GetMapping("/convert")
    @Operation(summary = "Convert amount between currencies")
    public ResponseEntity<ApiResponse<Map<String, Object>>> convert(
            @RequestParam BigDecimal amount,
            @RequestParam String from,
            @RequestParam String to) {
        BigDecimal converted = currencyService.convert(amount, from, to);
        Map<String, Object> result = Map.of(
                "originalAmount", amount,
                "fromCurrency", from.toUpperCase(),
                "toCurrency", to.toUpperCase(),
                "convertedAmount", converted
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping
    @Operation(summary = "List all active currencies")
    public ResponseEntity<ApiResponse<List<Currency>>> getAllCurrencies() {
        List<Currency> currencies = currencyService.getAllActiveCurrencies();
        return ResponseEntity.ok(ApiResponse.success(currencies));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get currency by code")
    public ResponseEntity<ApiResponse<Currency>> getCurrency(@PathVariable String code) {
        Currency currency = currencyService.getCurrencyByCode(code);
        return ResponseEntity.ok(ApiResponse.success(currency));
    }

    @PutMapping("/{code}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle currency active status")
    public ResponseEntity<ApiResponse<Currency>> toggleActive(@PathVariable String code) {
        Currency currency = currencyService.toggleActive(code);
        return ResponseEntity.ok(ApiResponse.success("Currency status toggled", currency));
    }
}