package com.ims.repository;

import com.ims.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findBySku(String sku);
    
    Optional<Product> findByBarcode(String barcode);
    
    boolean existsBySku(String sku);
    
    boolean existsByBarcode(String barcode);
    
    Page<Product> findByIsDeletedFalse(Pageable pageable);
    
    List<Product> findByCategoryIdAndIsDeletedFalse(Long categoryId);
    
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.barcode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.expiryDate IS NOT NULL AND p.expiryDate <= :date")
    List<Product> findProductsExpiringBefore(@Param("date") LocalDate date);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.expiryDate IS NOT NULL AND p.expiryDate BETWEEN :start AND :end")
    List<Product> findProductsExpiringBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.expiryDate IS NOT NULL AND p.expiryDate < :date")
    Page<Product> findExpiredProducts(@Param("date") LocalDate date, Pageable pageable);

    Optional<Product> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
