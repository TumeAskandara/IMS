package com.ims.service;

import com.ims.dto.customer.CustomerDTO;
import com.ims.dto.customer.CustomerRequest;
import com.ims.dto.customer.PurchaseHistoryDTO;
import com.ims.entity.Branch;
import com.ims.entity.Customer;
import com.ims.entity.Sale;
import com.ims.entity.User;
import com.ims.enums.CustomerStatus;
import com.ims.enums.CustomerType;
import com.ims.enums.NotificationPriority;
import com.ims.enums.NotificationType;
import com.ims.enums.Role;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.BranchRepository;
import com.ims.repository.CustomerRepository;
import com.ims.repository.SaleRepository;
import com.ims.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final SaleRepository saleRepository;
    private final MessageService messageService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public CustomerDTO createCustomer(CustomerRequest request) {
        log.info("Creating new customer: {}", request.getName());

        // Validate branch exists
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        // Validate unique email
        if (request.getEmail() != null && customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(messageService.getMessage("user.emailexists"));
        }

        // Generate customer ID
        String customerId = generateCustomerId();

        Customer customer = Customer.builder()
                .customerId(customerId)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .taxId(request.getTaxId())
                .customerType(CustomerType.valueOf(request.getCustomerType()))
                .status(CustomerStatus.ACTIVE)
                .creditLimit(request.getCreditLimit() != null ? request.getCreditLimit() : BigDecimal.ZERO)
                .currentDebt(BigDecimal.ZERO)  // ADD THIS LINE
                .branch(branch)
                .notes(request.getNotes())
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Customer created successfully: {}", saved.getCustomerId());

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return mapToDTO(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerDTO> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<CustomerDTO> searchCustomers(String query, Pageable pageable) {
        return customerRepository.searchCustomers(query, pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<CustomerDTO> getCustomersByBranch(Long branchId, Pageable pageable) {
        return customerRepository.findByBranchId(branchId, pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<CustomerDTO> getCustomersWithDebt(Pageable pageable) {
        return customerRepository.findCustomersWithDebt(pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public List<CustomerDTO> getTopCustomers(int limit) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        return customerRepository.findTopCustomers(pageable).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PurchaseHistoryDTO> getCustomerPurchaseHistory(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return saleRepository.findByCustomerOrderByCreatedAtDesc(customer).stream()
                .map(this::mapToPurchaseHistoryDTO)
                .collect(Collectors.toList());
    }

    public CustomerDTO updateCustomer(Long id, CustomerRequest request) {
        log.info("Updating customer: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Update fields
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setTaxId(request.getTaxId());
        customer.setCustomerType(CustomerType.valueOf(request.getCustomerType()));
        customer.setCreditLimit(request.getCreditLimit());
        customer.setNotes(request.getNotes());

        // Update branch if changed
        if (!customer.getBranch().getId().equals(request.getBranchId())) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
            customer.setBranch(branch);
        }

        Customer updated = customerRepository.save(customer);
        log.info("Customer updated successfully: {}", updated.getCustomerId());

        return mapToDTO(updated);
    }

    public void deleteCustomer(Long id) {
        log.info("Deleting customer: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        customerRepository.delete(customer);
        log.info("Customer deleted successfully: {}", customer.getCustomerId());
    }

    public void activateCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        customer.setStatus(CustomerStatus.ACTIVE);
        customerRepository.save(customer);
    }

    public void suspendCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        customer.setStatus(CustomerStatus.SUSPENDED);
        customerRepository.save(customer);
    }

    public void blacklistCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        customer.setStatus(CustomerStatus.BLACKLISTED);
        customerRepository.save(customer);
    }

    // Check credit limits when customer makes purchase
    public void checkCreditLimit(Customer customer) {
        BigDecimal availableCredit = customer.getAvailableCredit();
        BigDecimal creditLimit = customer.getCreditLimit();

        // If less than 20% credit remaining
        if (creditLimit.compareTo(BigDecimal.ZERO) > 0) {
            double percentageRemaining = availableCredit
                    .divide(creditLimit, 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();

            if (percentageRemaining < 20) {
                // Notify managers
                List<User> managers = userRepository.findByBranchAndRole(
                        customer.getBranch(),
                        Role.MANAGER
                );

                for (User manager : managers) {
                    notificationService.createNotification(
                            manager.getId(),
                            NotificationType.CREDIT_LIMIT_WARNING,
                            NotificationPriority.MEDIUM,
                            "Credit Limit Warning",
                            String.format("Customer '%s' has only $%.2f credit remaining (%.0f%%)",
                                    customer.getName(),
                                    availableCredit,
                                    percentageRemaining)
                    );
                }
            }
        }
    }

    // Helper methods
    private String generateCustomerId() {
        long count = customerRepository.count() + 1;
        return String.format("CUST-%05d", count);
    }

    private CustomerDTO mapToDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .customerId(customer.getCustomerId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .city(customer.getCity())
                .taxId(customer.getTaxId())
                .customerType(customer.getCustomerType().name())
                .status(customer.getStatus().name())
                .creditLimit(customer.getCreditLimit())
                .currentDebt(customer.getCurrentDebt())
                .availableCredit(customer.getAvailableCredit())
                .lifetimeValue(customer.getLifetimeValue())
                .totalPurchases(customer.getTotalPurchases())
                .lastPurchaseDate(customer.getLastPurchaseDate())
                .branchName(customer.getBranch().getName())
                .notes(customer.getNotes())
                .createdAt(customer.getCreatedAt())
                .build();
    }

    private PurchaseHistoryDTO mapToPurchaseHistoryDTO(Sale sale) {
        return PurchaseHistoryDTO.builder()
                .saleId(sale.getId())
                .invoiceNumber(sale.getInvoiceNumber())
                .saleDate(sale.getCreatedAt())
                .totalAmount(sale.getTotalAmount())
                .amountPaid(sale.getAmountPaid())
                .amountDue(sale.getTotalAmount().subtract(sale.getAmountPaid()))
                .paymentMethod(sale.getPaymentMethod().name())
                .status(sale.getStatus().name())
                .itemCount(sale.getSaleItems().size())
                .branchName(sale.getBranch().getName())
                .build();
    }
}