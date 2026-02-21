package com.ims.dto.expense;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSummaryDTO {
    private LocalDate date;
    private String branchName;
    private BigDecimal totalSales;
    private BigDecimal totalExpenses;
    private BigDecimal costOfGoodsSold;
    private BigDecimal netProfit; // Sales - COGS - Expenses
    private Map<String, BigDecimal> expensesByCategory;
}