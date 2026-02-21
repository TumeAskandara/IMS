package com.ims.repository;

import com.ims.entity.Branch;
import com.ims.entity.BranchInventory;
import com.ims.entity.Product;
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

    // ==========================================
    // BASIC FINDERS
    // ==========================================

    Optional<BranchInventory> findByBranchIdAndProductId(Long branchId, Long productId);

    Optional<BranchInventory> findByBranchAndProduct(Branch branch, Product product);

    List<BranchInventory> findByBranchId(Long branchId);

    List<BranchInventory> findByProductId(Long productId);

    Page<BranchInventory> findByBranchId(Long branchId, Pageable pageable);

    boolean existsByBranchIdAndProductId(Long branchId, Long productId);

    // ==========================================
    // STOCK CALCULATIONS
    // ==========================================

    @Query("SELECT SUM(bi.quantityOnHand) FROM BranchInventory bi WHERE bi.product.id = :productId")
    Integer getTotalStockForProduct(@Param("productId") Long productId);

    // ==========================================
    // SEARCH & FILTER
    // ==========================================

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

    // ==========================================
    // STOCK STATUS QUERIES
    // ==========================================

    // Low stock items (below reorder level)
    @Query("SELECT bi FROM BranchInventory bi WHERE bi.branch.id = :branchId AND " +
            "bi.quantityOnHand < bi.product.reorderLevel AND bi.quantityOnHand > 0")
    List<BranchInventory> findLowStockItems(@Param("branchId") Long branchId);

    // Out of stock items - Paginated (for specific branch)
    @Query("SELECT bi FROM BranchInventory bi WHERE bi.branch.id = :branchId AND " +
            "bi.quantityOnHand = 0")
    Page<BranchInventory> findOutOfStockItems(@Param("branchId") Long branchId, Pageable pageable);

    // Out of stock items - List (all branches)
    @Query("SELECT bi FROM BranchInventory bi WHERE bi.quantityOnHand = 0")
    List<BranchInventory> findAllOutOfStockItems();

    // In stock items - Paginated
    @Query("SELECT bi FROM BranchInventory bi WHERE bi.branch.id = :branchId AND " +
            "bi.quantityOnHand > 0")
    Page<BranchInventory> findInStockItems(@Param("branchId") Long branchId, Pageable pageable);
}