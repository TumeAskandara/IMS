package com.ims.repository;

import com.ims.entity.StockTakeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTakeItemRepository extends JpaRepository<StockTakeItem, Long> {

    List<StockTakeItem> findByStockTakeId(Long stockTakeId);

    List<StockTakeItem> findByStockTakeIdAndDiscrepancyNot(Long stockTakeId, Integer discrepancy);
}