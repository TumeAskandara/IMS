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
    private final SaleReturnRepository saleReturnRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final DebtRepository debtRepository;
    private final BranchInventoryRepository branchInventoryRepository;

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary(Long branchId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime now = LocalDateTime.now();

        BigDecimal grossRevenue;
        BigDecimal netRevenue;
        BigDecimal totalReturns;
        Long totalSales;
        BigDecimal stockValue;
        Integer lowStockCount;

        if (branchId != null) {
            // Branch-specific data
            BigDecimal gross = saleRepository.getTotalSalesForBranch(branchId, startOfMonth, now);
            grossRevenue = gross != null ? gross : BigDecimal.ZERO;

            BigDecimal net = saleRepository.getNetSalesForBranch(branchId, startOfMonth, now);
            netRevenue = net != null ? net : BigDecimal.ZERO;

            BigDecimal returns = saleRepository.getTotalReturnsForBranch(branchId, startOfMonth, now);
            totalReturns = returns != null ? returns : BigDecimal.ZERO;

            BigDecimal refunds = saleReturnRepository.getTotalRefundAmountForBranch(branchId, startOfMonth, now);
            if (refunds != null && refunds.compareTo(totalReturns) > 0) {
                totalReturns = refunds;
            }

            totalSales = saleRepository.getSalesCountSince(branchId, startOfMonth);
            stockValue = branchInventoryRepository.getTotalStockValueForBranch(branchId);
            lowStockCount = branchInventoryRepository.findLowStockItems(branchId).size();
        } else {
            // All branches aggregated data
            BigDecimal gross = saleRepository.getTotalSalesAllBranches(startOfMonth, now);
            grossRevenue = gross != null ? gross : BigDecimal.ZERO;

            BigDecimal net = saleRepository.getNetSalesAllBranches(startOfMonth, now);
            netRevenue = net != null ? net : BigDecimal.ZERO;

            BigDecimal returns = saleRepository.getTotalReturnsAllBranches(startOfMonth, now);
            totalReturns = returns != null ? returns : BigDecimal.ZERO;

            BigDecimal refunds = saleReturnRepository.getTotalRefundAmountAllBranches(startOfMonth, now);
            if (refunds != null && refunds.compareTo(totalReturns) > 0) {
                totalReturns = refunds;
            }

            totalSales = saleRepository.getSalesCountSinceAllBranches(startOfMonth);
            stockValue = branchInventoryRepository.getTotalStockValue();
            lowStockCount = branchInventoryRepository.findAllLowStockItems().size();
        }

        Long returnsCount = saleReturnRepository.getCompletedReturnsCount(startOfMonth, now);
        Long totalReturnsCount = returnsCount != null ? returnsCount : 0L;

        BigDecimal totalOutstanding = debtRepository.getTotalOutstandingDebt();
        BigDecimal overdueDebt = debtRepository.getTotalOverdueDebt();
        Long activeDebtsCount = debtRepository.getActiveDebtsCount();

        return DashboardSummary.builder()
                .totalRevenue(netRevenue)
                .grossRevenue(grossRevenue)
                .totalReturns(totalReturns)
                .totalReturnsCount(totalReturnsCount)
                .totalSales(totalSales != null ? totalSales : 0)
                .totalStockValue(stockValue != null ? stockValue : BigDecimal.ZERO)
                .totalOutstandingDebt(totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO)
                .overdueDebt(overdueDebt != null ? overdueDebt : BigDecimal.ZERO)
                .activeDebtsCount(activeDebtsCount != null ? activeDebtsCount : 0)
                .lowStockProductsCount(lowStockCount)
                .totalProducts((int) productRepository.count())
                .totalBranches((int) branchRepository.count())
                .build();
    }
}
