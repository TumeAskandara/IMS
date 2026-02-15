package com.ims.repository;

import com.ims.entity.BranchInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchInventoryRepository extends JpaRepository<BranchInventory, Long> {
    
    Optional<BranchInventory> findByBranchIdAndProductId(Long branchId, Long productId);
    
    List<BranchInventory> findByBranchId(Long branchId);
    
    List<BranchInventory> findByProductId(Long productId);
    
    Page<BranchInventory> findByBranchId(Long branchId, Pageable pageable);
    
    @Query("SELECT bi FROM BranchInventory bi WHERE bi.branch.id = :branchId AND " +
           "bi.quantityAvailable <= bi.product.reorderLevel")
    List<BranchInventory> findLowStockItems(@Param("branchId") Long branchId);
    
    @Query("SELECT SUM(bi.quantityOnHand) FROM BranchInventory bi WHERE bi.product.id = :productId")
    Integer getTotalStockForProduct(@Param("productId") Long productId);
    
    @Query("SELECT bi FROM BranchInventory bi WHERE bi.branch.id = :branchId AND " +
           "(LOWER(bi.product.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(bi.product.sku) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<BranchInventory> searchInventory(@Param("branchId") Long branchId, 
                                          @Param("searchTerm") String searchTerm, 
                                          Pageable pageable);
    
    @Query("SELECT bi FROM BranchInventory bi WHERE bi.branch.id = :branchId AND " +
           "bi.product.category.id = :categoryId")
    Page<BranchInventory> findByBranchIdAndCategoryId(@Param("branchId") Long branchId,
                                                       @Param("categoryId") Long categoryId,
                                                       Pageable pageable);
    
    @Query("SELECT bi FROM BranchInventory bi WHERE bi.branch.id = :branchId AND " +
           "bi.quantityAvailable > 0")
    Page<BranchInventory> findInStockItems(@Param("branchId") Long branchId, Pageable pageable);
    
    @Query("SELECT bi FROM BranchInventory bi WHERE bi.branch.id = :branchId AND " +
           "bi.quantityAvailable = 0")
    Page<BranchInventory> findOutOfStockItems(@Param("branchId") Long branchId, Pageable pageable);
}
