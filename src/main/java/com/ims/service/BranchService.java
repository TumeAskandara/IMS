package com.ims.service;

import com.ims.dto.request.BranchRequest;
import com.ims.entity.Branch;
import com.ims.exception.BadRequestException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public Page<Branch> getAllBranches(Pageable pageable) {
        return branchRepository.findByIsDeletedFalse(pageable);
    }

    @Transactional(readOnly = true)
    public Branch getBranchById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", id));
    }

    @Transactional
    public Branch createBranch(BranchRequest request) {
        if (branchRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Branch with code " + request.getCode() + " already exists");
        }

        Branch branch = Branch.builder()
                .code(request.getCode())
                .name(request.getName())
                .address(request.getAddress())
                .phoneNumber(request.getPhoneNumber())
                .city(request.getCity())
                .country(request.getCountry())
                .isActive(request.getIsActive())
                .build();

        return branchRepository.save(branch);
    }

    @Transactional
    public Branch updateBranch(Long id, BranchRequest request) {
        Branch branch = getBranchById(id);

        if (!branch.getCode().equals(request.getCode()) && 
            branchRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Branch with code " + request.getCode() + " already exists");
        }

        branch.setCode(request.getCode());
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setPhoneNumber(request.getPhoneNumber());
        branch.setCity(request.getCity());
        branch.setCountry(request.getCountry());
        branch.setIsActive(request.getIsActive());

        return branchRepository.save(branch);
    }

    @Transactional
    public void deleteBranch(Long id) {
        Branch branch = getBranchById(id);
        branch.setIsDeleted(true);
        branchRepository.save(branch);
    }
}
