package com.ims.repository;

import com.ims.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByName(String name);
    
    List<Category> findByParentIdAndIsDeletedFalse(Long parentId);
    
    List<Category> findByParentIsNullAndIsDeletedFalse();
    
    List<Category> findByIsDeletedFalse();
}
