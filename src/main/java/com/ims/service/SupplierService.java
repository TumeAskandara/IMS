package com.ims.service;

import com.ims.dto.supplier.SupplierDTO;
import com.ims.dto.supplier.SupplierRequest;
import com.ims.entity.Supplier;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierDTO createSupplier(SupplierRequest request) {
        Supplier supplier = Supplier.builder()
                .code(generateSupplierCode())
                .name(request.getName())
                .contactPerson(request.getContactPerson())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .paymentTerms(request.getPaymentTerms())
                .leadTimeDays(request.getLeadTimeDays())
                .rating(request.getRating())
                .build();

        supplier = supplierRepository.save(supplier);
        log.info("Created supplier: {} ({})", supplier.getName(), supplier.getCode());
        return mapToDTO(supplier);
    }

    @Transactional(readOnly = true)
    public SupplierDTO getSupplierById(Long id) {
        return mapToDTO(findById(id));
    }

    @Transactional(readOnly = true)
    public Page<SupplierDTO> getAllSuppliers(Pageable pageable) {
        return supplierRepository.findAll(pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<SupplierDTO> searchSuppliers(String search, Pageable pageable) {
        return supplierRepository.searchByNameOrContact(search, pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public List<SupplierDTO> getActiveSuppliers() {
        return supplierRepository.findByIsActiveTrue().stream().map(this::mapToDTO).toList();
    }

    public SupplierDTO updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = findById(id);
        supplier.setName(request.getName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setCity(request.getCity());
        supplier.setCountry(request.getCountry());
        supplier.setPaymentTerms(request.getPaymentTerms());
        supplier.setLeadTimeDays(request.getLeadTimeDays());
        supplier.setRating(request.getRating());

        supplier = supplierRepository.save(supplier);
        log.info("Updated supplier: {}", supplier.getCode());
        return mapToDTO(supplier);
    }

    public void deactivateSupplier(Long id) {
        Supplier supplier = findById(id);
        supplier.setIsActive(false);
        supplierRepository.save(supplier);
        log.info("Deactivated supplier: {}", supplier.getCode());
    }

    public void activateSupplier(Long id) {
        Supplier supplier = findById(id);
        supplier.setIsActive(true);
        supplierRepository.save(supplier);
        log.info("Activated supplier: {}", supplier.getCode());
    }

    private Supplier findById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
    }

    private String generateSupplierCode() {
        long count = supplierRepository.count() + 1;
        String code;
        do {
            code = String.format("SUP-%05d", count);
            count++;
        } while (supplierRepository.existsByCode(code));
        return code;
    }

    private SupplierDTO mapToDTO(Supplier supplier) {
        return SupplierDTO.builder()
                .id(supplier.getId())
                .code(supplier.getCode())
                .name(supplier.getName())
                .contactPerson(supplier.getContactPerson())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .city(supplier.getCity())
                .country(supplier.getCountry())
                .paymentTerms(supplier.getPaymentTerms() != null ? supplier.getPaymentTerms().name() : null)
                .leadTimeDays(supplier.getLeadTimeDays())
                .rating(supplier.getRating())
                .isActive(supplier.getIsActive())
                .createdAt(supplier.getCreatedAt())
                .build();
    }
}
