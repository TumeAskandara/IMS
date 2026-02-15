package com.ims.repository;

import com.ims.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    
    List<SaleItem> findBySaleId(Long saleId);
    
    @Query("SELECT si FROM SaleItem si WHERE si.product.id = :productId AND " +
           "si.sale.saleDate BETWEEN :start AND :end")
    List<SaleItem> findByProductAndDateRange(@Param("productId") Long productId, 
                                             @Param("start") LocalDateTime start, 
                                             @Param("end") LocalDateTime end);
}
