package com.ims.service;

import com.ims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GlobalSearchService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final CreditAccountRepository creditAccountRepository;

    public Map<String, Object> search(String query, int maxResultsPerType) {
        Pageable pageable = PageRequest.of(0, maxResultsPerType);
        Map<String, Object> results = new LinkedHashMap<>();

        var products = productRepository.searchProducts(query, pageable);
        if (products.hasContent()) {
            results.put("products", products.getContent());
        }

        var customers = customerRepository.searchCustomers(query, pageable);
        if (customers.hasContent()) {
            results.put("customers", customers.getContent());
        }

        var suppliers = supplierRepository.searchSuppliers(query, pageable);
        if (suppliers.hasContent()) {
            results.put("suppliers", suppliers.getContent());
        }

        var creditAccounts = creditAccountRepository.searchCreditAccounts(query, pageable);
        if (creditAccounts.hasContent()) {
            results.put("creditAccounts", creditAccounts.getContent());
        }

        return results;
    }
}