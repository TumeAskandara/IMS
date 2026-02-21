package com.ims.controller;

import com.ims.dto.analytics.*;
import com.ims.dto.response.ApiResponse;
import com.ims.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Analytics & Reporting", description = "APIs for analytics and business intelligence")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/sales-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get sales summary", description = "Get comprehensive sales summary for a date range")
    public ResponseEntity<ApiResponse<SalesSummaryDTO>> getSalesSummary(
            @Parameter(description = "Start date and time (ISO format)", example = "2026-02-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date and time (ISO format)", example = "2026-02-28T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Optional branch ID to filter by specific branch")
            @RequestParam(required = false) Long branchId) {

        SalesSummaryDTO summary = analyticsService.getSalesSummary(startDate, endDate, branchId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/inventory-valuation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get inventory valuation", description = "Get total inventory value and statistics")
    public ResponseEntity<ApiResponse<InventoryValuationDTO>> getInventoryValuation(
            @Parameter(description = "Optional branch ID to filter by specific branch")
            @RequestParam(required = false) Long branchId) {

        InventoryValuationDTO valuation = analyticsService.getInventoryValuation(branchId);
        return ResponseEntity.ok(ApiResponse.success(valuation));
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get top performing products", description = "Get list of best selling products")
    public ResponseEntity<ApiResponse<List<ProductPerformanceDTO>>> getTopProducts(
            @Parameter(description = "Start date and time (ISO format)", example = "2026-02-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date and time (ISO format)", example = "2026-02-28T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Maximum number of products to return", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        List<ProductPerformanceDTO> topProducts = analyticsService.getTopProducts(startDate, endDate, limit);
        return ResponseEntity.ok(ApiResponse.success(topProducts));
    }

    @GetMapping("/sales-trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get sales trends", description = "Get daily sales trends for visualization")
    public ResponseEntity<ApiResponse<List<SalesTrendDTO>>> getSalesTrends(
            @Parameter(description = "Start date and time (ISO format)", example = "2026-02-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date and time (ISO format)", example = "2026-02-28T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<SalesTrendDTO> trends = analyticsService.getSalesTrends(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(trends));
    }

    @GetMapping("/branch-performance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get branch performance", description = "Compare performance across all branches")
    public ResponseEntity<ApiResponse<List<BranchPerformanceDTO>>> getBranchPerformance(
            @Parameter(description = "Start date and time (ISO format)", example = "2026-02-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date and time (ISO format)", example = "2026-02-28T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<BranchPerformanceDTO> performance = analyticsService.getBranchPerformance(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(performance));
    }

    @GetMapping("/profit-analysis")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get profit analysis", description = "Detailed profit and margin analysis")
    public ResponseEntity<ApiResponse<ProfitAnalysisDTO>> getProfitAnalysis(
            @Parameter(description = "Start date and time (ISO format)", example = "2026-02-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date and time (ISO format)", example = "2026-02-28T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        ProfitAnalysisDTO analysis = analyticsService.getProfitAnalysis(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }

    @GetMapping("/customer-insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get customer insights", description = "Get customer behavior and value analysis")
    public ResponseEntity<ApiResponse<List<CustomerInsightDTO>>> getCustomerInsights(
            @Parameter(description = "Maximum number of customers to return", example = "20")
            @RequestParam(defaultValue = "20") int limit) {

        List<CustomerInsightDTO> insights = analyticsService.getCustomerInsights(limit);
        return ResponseEntity.ok(ApiResponse.success(insights));
    }

    // Quick stats endpoints for dashboard

    @GetMapping("/dashboard/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get today's summary", description = "Quick summary of today's performance")
    public ResponseEntity<ApiResponse<SalesSummaryDTO>> getTodaySummary(
            @Parameter(description = "Optional branch ID to filter by specific branch")
            @RequestParam(required = false) Long branchId) {

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        SalesSummaryDTO summary = analyticsService.getSalesSummary(startOfDay, endOfDay, branchId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/dashboard/this-month")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get this month's summary", description = "Summary of current month performance")
    public ResponseEntity<ApiResponse<SalesSummaryDTO>> getThisMonthSummary(
            @Parameter(description = "Optional branch ID to filter by specific branch")
            @RequestParam(required = false) Long branchId) {

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();

        SalesSummaryDTO summary = analyticsService.getSalesSummary(startOfMonth, now, branchId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/dashboard/this-year")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get this year's summary", description = "Summary of current year performance")
    public ResponseEntity<ApiResponse<SalesSummaryDTO>> getThisYearSummary(
            @Parameter(description = "Optional branch ID to filter by specific branch")
            @RequestParam(required = false) Long branchId) {

        LocalDateTime startOfYear = LocalDateTime.now().withDayOfYear(1)
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();

        SalesSummaryDTO summary = analyticsService.getSalesSummary(startOfYear, now, branchId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}