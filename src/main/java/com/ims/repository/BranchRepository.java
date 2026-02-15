package com.ims.repository;

import com.ims.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    
    Optional<Branch> findByCode(String code);
    
    boolean existsByCode(String code);
    
    List<Branch> findByIsActiveTrueAndIsDeletedFalse();
    
    Page<Branch> findByIsDeletedFalse(Pageable pageable);
}
