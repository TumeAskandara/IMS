package com.ims.service;

import com.ims.dto.analytics.*;
import com.ims.entity.*;
import com.ims.enums.ReturnStatus;
import com.ims.enums.SaleStatus;
import com.ims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final BranchInventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final SaleItemRepository saleItemRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final ExpenseRepository expenseRepository; // ADD THIS

    /**
     * Get sales summary for a date range (accounting for returns AND expenses)
     */
    public SalesSummaryDTO getSalesSummary(LocalDateTime startDate, LocalDateTime endDate, Long branchId) {
        log.info("Generating sales summary from {} to {}", startDate, endDate);

        List<Sale> sales;
        List<SaleReturn> returns;
        List<Expense> expenses;

        if (branchId != null) {
            sales = saleRepository.findByBranchIdAndSaleDateBetween(branchId, startDate, endDate);
            returns = saleReturnRepository.findByBranchIdAndReturnDateBetween(branchId, startDate, endDate);
            expenses = expenseRepository.findByBranchAndDateRange(
                    branchId,
                    startDate.toLocalDate(),
                    endDate.toLocalDate()
            );
        } else {
            sales = saleRepository.findAll().stream()
                    .filter(s -> !s.getSaleDate().isBefore(startDate) && !s.getSaleDate().isAfter(endDate))
                    .filter(s -> s.getStatus() == SaleStatus.COMPLETED)
                    .collect(Collectors.toList());

            returns = saleReturnRepository.findAll().stream()
                    .filter(r -> !r.getReturnDate().isBefore(startDate) && !r.getReturnDate().isAfter(endDate))
                    .filter(r -> r.getStatus() == ReturnStatus.COMPLETED)
                    .collect(Collectors.toList());

            expenses = expenseRepository.findByBranchAndDateRange(
                    null,
                    startDate.toLocalDate(),
                    endDate.toLocalDate()
            );
        }

        // Calculate gross sales
        BigDecimal grossRevenue = sales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = sales.stream()
                .flatMap(sale -> sale.getSaleItems().stream())
                .map(item -> item.getProduct().getCostPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate returns
        BigDecimal totalReturns = returns.stream()
                .map(SaleReturn::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal returnedCost = returns.stream()
                .flatMap(ret -> ret.getItems().stream())
                .map(item -> item.getProduct().getCostPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantityReturned())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate expenses
        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Net calculations (after returns and expenses)
        BigDecimal netRevenue = grossRevenue.subtract(totalReturns);
        BigDecimal netCost = totalCost.subtract(returnedCost);
        BigDecimal grossProfit = netRevenue.subtract(netCost);
        BigDecimal netProfit = grossProfit.subtract(totalExpenses); // TRUE PROFIT after expenses

        int totalItemsSold = sales.stream()
                .flatMap(sale -> sale.getSaleItems().stream())
                .mapToInt(SaleItem::getQuantity)
                .sum();

        int totalItemsReturned = returns.stream()
                .flatMap(ret -> ret.getItems().stream())
                .mapToInt(SaleReturnItem::getQuantityReturned)
                .sum();

        int netItemsSold = totalItemsSold - totalItemsReturned;

        Long uniqueCustomers = sales.stream()
                .map(Sale::getCustomer)
                .filter(Objects::nonNull)
                .map(customer -> customer.getId())
                .distinct()
                .count();

        BigDecimal cashSales = sales.stream()
                .filter(s -> "CASH".equals(s.getPaymentMethod().name()))
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditSales = sales.stream()
                .filter(s -> "CREDIT".equals(s.getPaymentMethod().name()))
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cardSales = sales.stream()
                .filter(s -> "CARD".equals(s.getPaymentMethod().name()))
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgSaleValue = sales.isEmpty() ? BigDecimal.ZERO :
                netRevenue.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP);

        double profitMargin = netRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                netProfit.divide(netRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;

        return SalesSummaryDTO.builder()
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalSales((long) sales.size())
                .totalRevenue(netRevenue) // NET revenue (after returns)
                .totalCost(netCost) // NET cost (after returns)
                .totalExpenses(totalExpenses) // NEW: Total expenses
                .totalProfit(netProfit) // NEW: TRUE NET profit (Revenue - COGS - Expenses)
                .averageSaleValue(avgSaleValue)
                .totalItemsSold(netItemsSold) // NET items sold (after returns)
                .uniqueCustomers(uniqueCustomers)
                .cashSales(cashSales)
                .creditSales(creditSales)
                .cardSales(cardSales)
                .profitMargin(profitMargin)
                .build();
    }

    /**
     * Get inventory valuation
     */
    public InventoryValuationDTO getInventoryValuation(Long branchId) {
        log.info("Generating inventory valuation for branch {}", branchId);

        List<BranchInventory> inventories;
        if (branchId != null) {
            inventories = inventoryRepository.findByBranchId(branchId);
        } else {
            inventories = inventoryRepository.findAll();
        }

        BigDecimal totalValue = inventories.stream()
                .map(inv -> inv.getProduct().getSellingPrice()
                        .multiply(BigDecimal.valueOf(inv.getQuantityAvailable())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = inventories.stream()
                .map(inv -> inv.getProduct().getCostPrice()
                        .multiply(BigDecimal.valueOf(inv.getQuantityAvailable())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQuantity = inventories.stream()
                .mapToInt(BranchInventory::getQuantityAvailable)
                .sum();

        long lowStockProducts = inventories.stream()
                .filter(inv -> inv.getProduct().getReorderLevel() != null &&
                        inv.getQuantityAvailable() < inv.getProduct().getReorderLevel())
                .count();

        long outOfStockProducts = inventories.stream()
                .filter(inv -> inv.getQuantityAvailable() == 0)
                .count();

        Set<Long> uniqueProducts = inventories.stream()
                .map(inv -> inv.getProduct().getId())
                .collect(Collectors.toSet());

        Set<Long> uniqueCategories = inventories.stream()
                .map(inv -> inv.getProduct().getCategory().getId())
                .collect(Collectors.toSet());

        BigDecimal avgProductValue = uniqueProducts.isEmpty() ? BigDecimal.ZERO :
                totalValue.divide(BigDecimal.valueOf(uniqueProducts.size()), 2, RoundingMode.HALF_UP);

        Optional<BranchInventory> mostValuable = inventories.stream()
                .max(Comparator.comparing(inv -> inv.getProduct().getSellingPrice()
                        .multiply(BigDecimal.valueOf(inv.getQuantityAvailable()))));

        String mostValuableProduct = mostValuable.map(inv -> inv.getProduct().getName()).orElse("N/A");
        BigDecimal mostValuableProductValue = mostValuable
                .map(inv -> inv.getProduct().getSellingPrice()
                        .multiply(BigDecimal.valueOf(inv.getQuantityAvailable())))
                .orElse(BigDecimal.ZERO);

        return InventoryValuationDTO.builder()
                .totalProducts((long) uniqueProducts.size())
                .totalCategories((long) uniqueCategories.size())
                .totalQuantity(totalQuantity)
                .totalValue(totalValue)
                .totalCost(totalCost)
                .lowStockProducts(lowStockProducts)
                .outOfStockProducts(outOfStockProducts)
                .averageProductValue(avgProductValue)
                .mostValuableProduct(mostValuableProduct)
                .mostValuableProductValue(mostValuableProductValue)
                .build();
    }

    /**
     * Get top performing products (accounting for returns)
     */
    public List<ProductPerformanceDTO> getTopProducts(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        log.info("Getting top {} products from {} to {}", limit, startDate, endDate);

        List<Sale> sales = saleRepository.findAll().stream()
                .filter(s -> !s.getSaleDate().isBefore(startDate) && !s.getSaleDate().isAfter(endDate))
                .filter(s -> s.getStatus() == SaleStatus.COMPLETED)
                .collect(Collectors.toList());

        List<SaleReturn> returns = saleReturnRepository.findAll().stream()
                .filter(r -> !r.getReturnDate().isBefore(startDate) && !r.getReturnDate().isAfter(endDate))
                .filter(r -> r.getStatus() == ReturnStatus.COMPLETED)
                .collect(Collectors.toList());

        Map<Long, ProductPerformanceDTO> productStats = new HashMap<>();

        // Process sales
        for (Sale sale : sales) {
            for (SaleItem item : sale.getSaleItems()) {
                Product product = item.getProduct();
                Long productId = product.getId();

                ProductPerformanceDTO stats = productStats.getOrDefault(productId,
                        ProductPerformanceDTO.builder()
                                .productId(productId)
                                .productName(product.getName())
                                .categoryName(product.getCategory().getName())
                                .quantitySold(0)
                                .totalRevenue(BigDecimal.ZERO)
                                .totalProfit(BigDecimal.ZERO)
                                .numberOfSales(0L)
                                .build());

                stats.setQuantitySold(stats.getQuantitySold() + item.getQuantity());
                stats.setTotalRevenue(stats.getTotalRevenue().add(item.getLineTotal()));
                stats.setNumberOfSales(stats.getNumberOfSales() + 1);

                BigDecimal itemCost = product.getCostPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                stats.setTotalProfit(stats.getTotalProfit().add(item.getLineTotal().subtract(itemCost)));

                productStats.put(productId, stats);
            }
        }

        // Subtract returns
        for (SaleReturn saleReturn : returns) {
            for (SaleReturnItem item : saleReturn.getItems()) {
                Product product = item.getProduct();
                Long productId = product.getId();

                ProductPerformanceDTO stats = productStats.get(productId);
                if (stats != null) {
                    stats.setQuantitySold(stats.getQuantitySold() - item.getQuantityReturned());
                    stats.setTotalRevenue(stats.getTotalRevenue().subtract(item.getSubtotal()));

                    BigDecimal returnedCost = product.getCostPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantityReturned()));
                    stats.setTotalProfit(stats.getTotalProfit().subtract(item.getSubtotal().subtract(returnedCost)));
                }
            }
        }

        // Calculate averages and margins
        productStats.values().forEach(stats -> {
            if (stats.getQuantitySold() > 0) {
                stats.setAverageSellingPrice(
                        stats.getTotalRevenue().divide(
                                BigDecimal.valueOf(stats.getQuantitySold()), 2, RoundingMode.HALF_UP));
            }
            if (stats.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0) {
                stats.setProfitMargin(
                        stats.getTotalProfit().divide(stats.getTotalRevenue(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100)).doubleValue());
            }
        });

        return productStats.values().stream()
                .sorted(Comparator.comparing(ProductPerformanceDTO::getTotalRevenue).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get sales trends (daily breakdown accounting for returns AND expenses)
     */
    public List<SalesTrendDTO> getSalesTrends(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Getting sales trends from {} to {}", startDate, endDate);

        List<Sale> sales = saleRepository.findAll().stream()
                .filter(s -> !s.getSaleDate().isBefore(startDate) && !s.getSaleDate().isAfter(endDate))
                .filter(s -> s.getStatus() == SaleStatus.COMPLETED)
                .collect(Collectors.toList());

        List<SaleReturn> returns = saleReturnRepository.findAll().stream()
                .filter(r -> !r.getReturnDate().isBefore(startDate) && !r.getReturnDate().isAfter(endDate))
                .filter(r -> r.getStatus() == ReturnStatus.COMPLETED)
                .collect(Collectors.toList());

        List<Expense> expenses = expenseRepository.findByBranchAndDateRange(
                null,
                startDate.toLocalDate(),
                endDate.toLocalDate()
        );

        Map<LocalDate, List<Sale>> salesByDate = sales.stream()
                .collect(Collectors.groupingBy(s -> s.getSaleDate().toLocalDate()));

        Map<LocalDate, List<SaleReturn>> returnsByDate = returns.stream()
                .collect(Collectors.groupingBy(r -> r.getReturnDate().toLocalDate()));

        Map<LocalDate, List<Expense>> expensesByDate = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getExpenseDate));

        // Get all dates in range
        Set<LocalDate> allDates = new HashSet<>();
        allDates.addAll(salesByDate.keySet());
        allDates.addAll(returnsByDate.keySet());
        allDates.addAll(expensesByDate.keySet());

        return allDates.stream()
                .map(date -> {
                    List<Sale> daySales = salesByDate.getOrDefault(date, Collections.emptyList());
                    List<SaleReturn> dayReturns = returnsByDate.getOrDefault(date, Collections.emptyList());
                    List<Expense> dayExpenses = expensesByDate.getOrDefault(date, Collections.emptyList());

                    BigDecimal dayGrossRevenue = daySales.stream()
                            .map(Sale::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal dayReturnsAmount = dayReturns.stream()
                            .map(SaleReturn::getRefundAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal dayNetRevenue = dayGrossRevenue.subtract(dayReturnsAmount);

                    BigDecimal dayCost = daySales.stream()
                            .flatMap(sale -> sale.getSaleItems().stream())
                            .map(item -> item.getProduct().getCostPrice()
                                    .multiply(BigDecimal.valueOf(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal dayReturnedCost = dayReturns.stream()
                            .flatMap(ret -> ret.getItems().stream())
                            .map(item -> item.getProduct().getCostPrice()
                                    .multiply(BigDecimal.valueOf(item.getQuantityReturned())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal dayNetCost = dayCost.subtract(dayReturnedCost);

                    // NEW: Calculate day expenses
                    BigDecimal dayTotalExpenses = dayExpenses.stream()
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    int itemsSold = daySales.stream()
                            .flatMap(sale -> sale.getSaleItems().stream())
                            .mapToInt(SaleItem::getQuantity)
                            .sum();

                    int itemsReturned = dayReturns.stream()
                            .flatMap(ret -> ret.getItems().stream())
                            .mapToInt(SaleReturnItem::getQuantityReturned)
                            .sum();

                    BigDecimal avgSaleValue = daySales.isEmpty() ? BigDecimal.ZERO :
                            dayNetRevenue.divide(BigDecimal.valueOf(daySales.size()), 2, RoundingMode.HALF_UP);

                    // NEW: Net profit includes expenses
                    BigDecimal dayNetProfit = dayNetRevenue.subtract(dayNetCost).subtract(dayTotalExpenses);

                    return SalesTrendDTO.builder()
                            .date(date)
                            .numberOfSales((long) daySales.size())
                            .totalRevenue(dayNetRevenue)
                            .totalProfit(dayNetProfit) // NOW includes expenses
                            .itemsSold(itemsSold - itemsReturned)
                            .averageSaleValue(avgSaleValue)
                            .build();
                })
                .sorted(Comparator.comparing(SalesTrendDTO::getDate))
                .collect(Collectors.toList());
    }

    /**
     * Get branch performance comparison (accounting for returns AND expenses)
     */
    public List<BranchPerformanceDTO> getBranchPerformance(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating branch performance report");

        return branchRepository.findAll().stream()
                .map(branch -> {
                    List<Sale> branchSales = saleRepository.findByBranchIdAndSaleDateBetween(
                            branch.getId(), startDate, endDate);

                    List<SaleReturn> branchReturns = saleReturnRepository.findByBranchIdAndReturnDateBetween(
                            branch.getId(), startDate, endDate);

                    List<Expense> branchExpenses = expenseRepository.findByBranchAndDateRange(
                            branch.getId(),
                            startDate.toLocalDate(),
                            endDate.toLocalDate()
                    );

                    BigDecimal grossRevenue = branchSales.stream()
                            .map(Sale::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal returnsAmount = branchReturns.stream()
                            .map(SaleReturn::getRefundAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal netRevenue = grossRevenue.subtract(returnsAmount);

                    BigDecimal totalCost = branchSales.stream()
                            .flatMap(sale -> sale.getSaleItems().stream())
                            .map(item -> item.getProduct().getCostPrice()
                                    .multiply(BigDecimal.valueOf(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal returnedCost = branchReturns.stream()
                            .flatMap(ret -> ret.getItems().stream())
                            .map(item -> item.getProduct().getCostPrice()
                                    .multiply(BigDecimal.valueOf(item.getQuantityReturned())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal netCost = totalCost.subtract(returnedCost);

                    // NEW: Calculate branch expenses
                    BigDecimal totalExpenses = branchExpenses.stream()
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // NEW: True profit includes expenses
                    BigDecimal totalProfit = netRevenue.subtract(netCost).subtract(totalExpenses);

                    int inventoryCount = inventoryRepository.findByBranchId(branch.getId()).size();

                    Long activeCustomers = customerRepository.countByBranchAndStatus(
                            branch.getId(),
                            com.ims.enums.CustomerStatus.ACTIVE);

                    BigDecimal avgSaleValue = branchSales.isEmpty() ? BigDecimal.ZERO :
                            netRevenue.divide(BigDecimal.valueOf(branchSales.size()), 2, RoundingMode.HALF_UP);

                    double profitMargin = netRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                            totalProfit.divide(netRevenue, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;

                    return BranchPerformanceDTO.builder()
                            .branchId(branch.getId())
                            .branchName(branch.getName())
                            .totalSales((long) branchSales.size())
                            .totalRevenue(netRevenue)
                            .totalProfit(totalProfit) // NOW includes expenses
                            .totalProducts(inventoryCount)
                            .activeCustomers(activeCustomers)
                            .averageSaleValue(avgSaleValue)
                            .profitMargin(profitMargin)
                            .build();
                })
                .sorted(Comparator.comparing(BranchPerformanceDTO::getTotalRevenue).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get profit analysis (accounting for returns AND expenses)
     */
    public ProfitAnalysisDTO getProfitAnalysis(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating profit analysis from {} to {}", startDate, endDate);

        List<Sale> sales = saleRepository.findAll().stream()
                .filter(s -> !s.getSaleDate().isBefore(startDate) && !s.getSaleDate().isAfter(endDate))
                .filter(s -> s.getStatus() == SaleStatus.COMPLETED)
                .collect(Collectors.toList());

        List<SaleReturn> returns = saleReturnRepository.findAll().stream()
                .filter(r -> !r.getReturnDate().isBefore(startDate) && !r.getReturnDate().isAfter(endDate))
                .filter(r -> r.getStatus() == ReturnStatus.COMPLETED)
                .collect(Collectors.toList());

        List<Expense> expenses = expenseRepository.findByBranchAndDateRange(
                null,
                startDate.toLocalDate(),
                endDate.toLocalDate()
        );

        BigDecimal grossRevenue = sales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal returnsAmount = returns.stream()
                .map(SaleReturn::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = grossRevenue.subtract(returnsAmount);

        BigDecimal totalCost = sales.stream()
                .flatMap(sale -> sale.getSaleItems().stream())
                .map(item -> item.getProduct().getCostPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal returnedCost = returns.stream()
                .flatMap(ret -> ret.getItems().stream())
                .map(item -> item.getProduct().getCostPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantityReturned())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netCost = totalCost.subtract(returnedCost);

        BigDecimal grossProfit = totalRevenue.subtract(netCost);

        // NEW: Operating expenses from expense records
        BigDecimal operatingExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netProfit = grossProfit.subtract(operatingExpenses);

        double grossMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                grossProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;

        double netMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                netProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;

        List<ProductPerformanceDTO> products = getTopProducts(startDate, endDate, 100);

        String mostProfitableProduct = products.isEmpty() ? "N/A" : products.get(0).getProductName();
        BigDecimal mostProfitableProductProfit = products.isEmpty() ? BigDecimal.ZERO :
                products.get(0).getTotalProfit();

        String leastProfitableProduct = products.size() < 2 ? "N/A" :
                products.get(products.size() - 1).getProductName();
        BigDecimal leastProfitableProductProfit = products.size() < 2 ? BigDecimal.ZERO :
                products.get(products.size() - 1).getTotalProfit();

        List<BranchPerformanceDTO> branches = getBranchPerformance(startDate, endDate);
        String mostProfitableBranch = branches.isEmpty() ? "N/A" : branches.get(0).getBranchName();
        BigDecimal mostProfitableBranchProfit = branches.isEmpty() ? BigDecimal.ZERO :
                branches.get(0).getTotalProfit();

        return ProfitAnalysisDTO.builder()
                .totalRevenue(totalRevenue)
                .totalCost(netCost)
                .grossProfit(grossProfit)
                .netProfit(netProfit) // NOW includes operating expenses
                .grossProfitMargin(grossMargin)
                .netProfitMargin(netMargin)
                .operatingExpenses(operatingExpenses) // NOW calculated from expense records
                .mostProfitableProduct(mostProfitableProduct)
                .mostProfitableProductProfit(mostProfitableProductProfit)
                .leastProfitableProduct(leastProfitableProduct)
                .leastProfitableProductProfit(leastProfitableProductProfit)
                .mostProfitableBranch(mostProfitableBranch)
                .mostProfitableBranchProfit(mostProfitableBranchProfit)
                .build();
    }

    /**
     * Get customer insights
     */
    public List<CustomerInsightDTO> getCustomerInsights(int limit) {
        log.info("Getting customer insights");

        return customerRepository.findAll().stream()
                .map(customer -> {
                    int daysSinceLastPurchase = customer.getLastPurchaseDate() != null ?
                            (int) ChronoUnit.DAYS.between(customer.getLastPurchaseDate(), LocalDateTime.now()) : 999;

                    BigDecimal avgPurchaseValue = customer.getTotalPurchases() > 0 ?
                            customer.getLifetimeValue().divide(
                                    BigDecimal.valueOf(customer.getTotalPurchases()), 2, RoundingMode.HALF_UP) :
                            BigDecimal.ZERO;

                    return CustomerInsightDTO.builder()
                            .customerId(customer.getId())
                            .customerName(customer.getName())
                            .customerType(customer.getCustomerType().name())
                            .lifetimeValue(customer.getLifetimeValue())
                            .totalPurchases(customer.getTotalPurchases())
                            .averagePurchaseValue(avgPurchaseValue)
                            .currentDebt(customer.getCurrentDebt())
                            .lastPurchaseDate(customer.getLastPurchaseDate())
                            .daysSinceLastPurchase(daysSinceLastPurchase)
                            .build();
                })
                .sorted(Comparator.comparing(CustomerInsightDTO::getLifetimeValue).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
