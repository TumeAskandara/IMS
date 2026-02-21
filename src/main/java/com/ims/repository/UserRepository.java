package com.ims.repository;

import com.ims.entity.Branch;
import com.ims.entity.User;
import com.ims.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    List<User> findByRole(Role role);
    
    Page<User> findByIsDeletedFalse(Pageable pageable);
    
    List<User> findByBranchIdAndIsDeletedFalse(Long branchId);
    
    Page<User> findByRoleAndIsDeletedFalse(Role role, Pageable pageable);

    List<User> findByBranchAndRole(Branch branch, Role role);
}
