package com.ims.service;

import com.ims.dto.expense.*;
import com.ims.entity.*;
import com.ims.enums.ExpenseCategory;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final SaleRepository saleRepository;

    public ExpenseDTO createExpense(ExpenseRequest request, Long userId, Long branchId) {
        log.info("Recording expense for branch: {}", branchId);

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", branchId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Expense expense = new Expense();
        expense.setBranch(branch);
        expense.setRecordedBy(user);
        expense.setCategory(ExpenseCategory.valueOf(request.getCategory()));
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setNotes(request.getNotes());
        expense.setCreatedBy(user.getUsername());
        expense.setIsDeleted(false);

        Expense saved = expenseRepository.save(expense);
        log.info("Expense recorded: {}", saved.getId());

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public ExpenseDTO getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id));
        return mapToDTO(expense);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseDTO> getExpensesByBranch(Long branchId, Pageable pageable) {
        return expenseRepository.findByBranchIdAndIsDeletedFalseOrderByExpenseDateDesc(branchId, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryDTO getDailySummary(Long branchId, LocalDate date) {
        log.info("Getting daily summary for branch {} on {}", branchId, date);

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", branchId));

        // Get expenses for the day
        List<Expense> expenses = expenseRepository.findByBranchAndDateRange(branchId, date, date);

        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by category
        Map<String, BigDecimal> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().name(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        // Get sales for the same day
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<Sale> sales = saleRepository.findByBranchIdAndSaleDateBetween(branchId, startOfDay, endOfDay);

        BigDecimal totalSales = sales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costOfGoodsSold = sales.stream()
                .flatMap(sale -> sale.getSaleItems().stream())
                .map(item -> item.getProduct().getCostPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Net Profit = Sales - COGS - Expenses
        BigDecimal netProfit = totalSales.subtract(costOfGoodsSold).subtract(totalExpenses);

        return ExpenseSummaryDTO.builder()
                .date(date)
                .branchName(branch.getName())
                .totalSales(totalSales)
                .totalExpenses(totalExpenses)
                .costOfGoodsSold(costOfGoodsSold)
                .netProfit(netProfit)
                .expensesByCategory(expensesByCategory)
                .build();
    }

    public void deleteExpense(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id));
        expense.setIsDeleted(true);
        expenseRepository.save(expense);
    }

    private ExpenseDTO mapToDTO(Expense expense) {
        return ExpenseDTO.builder()
                .id(expense.getId())
                .branchName(expense.getBranch().getName())
                .category(expense.getCategory().name())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .notes(expense.getNotes())
                .recordedByName(expense.getRecordedBy().getFullName())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}