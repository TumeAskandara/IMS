package com.ims.service;

import com.ims.dto.response.DashboardSummary;
import com.ims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final DebtRepository debtRepository;
    private final BranchInventoryRepository branchInventoryRepository;

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary(Long branchId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime now = LocalDateTime.now();

        BigDecimal totalRevenue = branchId != null ? 
                saleRepository.getTotalSalesForBranch(branchId, startOfMonth, now) :
                BigDecimal.ZERO;

        Long totalSales = branchId != null ?
                saleRepository.getTodaySalesCount(branchId, startOfMonth) :
                0L;

        BigDecimal totalOutstanding = debtRepository.getTotalOutstandingDebt();
        BigDecimal overdueDebt = debtRepository.getTotalOverdueDebt();
        Long activeDebtsCount = debtRepository.getActiveDebtsCount();

        Integer lowStockCount = branchId != null ?
                branchInventoryRepository.findLowStockItems(branchId).size() :
                0;

        return DashboardSummary.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalSales(totalSales != null ? totalSales : 0)
                .totalStockValue(BigDecimal.ZERO) // Calculate if needed
                .totalOutstandingDebt(totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO)
                .overdueDebt(overdueDebt != null ? overdueDebt : BigDecimal.ZERO)
                .activeDebtsCount(activeDebtsCount != null ? activeDebtsCount : 0)
                .lowStockProductsCount(lowStockCount)
                .totalProducts((int) productRepository.count())
                .totalBranches((int) branchRepository.count())
                .build();
    }
}
