package com.ims.repository;

import com.ims.entity.StockTake;
import com.ims.enums.StockTakeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockTakeRepository extends JpaRepository<StockTake, Long> {

    @Query("SELECT st FROM StockTake st LEFT JOIN FETCH st.items WHERE st.id = :id")
    Optional<StockTake> findByIdWithItems(@Param("id") Long id);

    Page<StockTake> findByBranchIdOrderByCreatedAtDesc(Long branchId, Pageable pageable);

    Page<StockTake> findByStatusOrderByCreatedAtDesc(StockTakeStatus status, Pageable pageable);

    Page<StockTake> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<StockTake> findByStockTakeNumber(String stockTakeNumber);
}