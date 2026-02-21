package com.ims.controller;

import com.ims.dto.customer.CustomerDTO;
import com.ims.dto.customer.CustomerRequest;
import com.ims.dto.customer.PurchaseHistoryDTO;
import com.ims.dto.response.ApiResponse;
import com.ims.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "APIs for managing customers")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create new customer", description = "Create a new customer in the system")
    public ResponseEntity<ApiResponse<CustomerDTO>> createCustomer(@Valid @RequestBody CustomerRequest request) {
        CustomerDTO customer = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", customer));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get all customers", description = "Retrieve paginated list of all customers")
    public ResponseEntity<ApiResponse<Page<CustomerDTO>>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<CustomerDTO> customers = customerService.getAllCustomers(pageable);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get customer by ID", description = "Retrieve customer details by ID")
    public ResponseEntity<ApiResponse<CustomerDTO>> getCustomerById(@PathVariable Long id) {
        CustomerDTO customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update customer", description = "Update existing customer information")
    public ResponseEntity<ApiResponse<CustomerDTO>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        
        CustomerDTO customer = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", customer));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete customer", description = "Delete a customer from the system")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Search customers", description = "Search customers by name, email, phone, or customer ID")
    public ResponseEntity<ApiResponse<Page<CustomerDTO>>> searchCustomers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerDTO> customers = customerService.searchCustomers(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get customer purchase history", description = "Retrieve all purchases made by a customer")
    public ResponseEntity<ApiResponse<List<PurchaseHistoryDTO>>> getCustomerHistory(@PathVariable Long id) {
        List<PurchaseHistoryDTO> history = customerService.getCustomerPurchaseHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/top")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get top customers", description = "Retrieve top customers by lifetime value")
    public ResponseEntity<ApiResponse<List<CustomerDTO>>> getTopCustomers(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<CustomerDTO> topCustomers = customerService.getTopCustomers(limit);
        return ResponseEntity.ok(ApiResponse.success(topCustomers));
    }

    @GetMapping("/with-debt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get customers with debt", description = "Retrieve all customers with outstanding debt")
    public ResponseEntity<ApiResponse<Page<CustomerDTO>>> getCustomersWithDebt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerDTO> customers = customerService.getCustomersWithDebt(pageable);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get customers by branch", description = "Retrieve customers for a specific branch")
    public ResponseEntity<ApiResponse<Page<CustomerDTO>>> getCustomersByBranch(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerDTO> customers = customerService.getCustomersByBranch(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Activate customer", description = "Activate a suspended or inactive customer")
    public ResponseEntity<ApiResponse<Void>> activateCustomer(@PathVariable Long id) {
        customerService.activateCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer activated successfully", null));
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Suspend customer", description = "Suspend a customer temporarily")
    public ResponseEntity<ApiResponse<Void>> suspendCustomer(@PathVariable Long id) {
        customerService.suspendCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer suspended successfully", null));
    }

    @PutMapping("/{id}/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Blacklist customer", description = "Blacklist a customer (ban from purchases)")
    public ResponseEntity<ApiResponse<Void>> blacklistCustomer(@PathVariable Long id) {
        customerService.blacklistCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer blacklisted successfully",null));
    }
}
