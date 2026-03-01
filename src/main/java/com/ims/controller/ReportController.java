package com.ims.controller;

import com.ims.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports", description = "APIs for generating downloadable reports")
public class ReportController {

    private final ReportService reportService;

    // ==========================================
    // SALES REPORTS
    // ==========================================

    @GetMapping("/sales/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Download sales report as Excel")
    public ResponseEntity<byte[]> salesReportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId) {
        byte[] data = reportService.generateSalesReportExcel(startDate, endDate, branchId);
        return buildExcelResponse(data, "sales_report_" + startDate + "_" + endDate + ".xlsx");
    }

    @GetMapping("/sales/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Download sales report as PDF")
    public ResponseEntity<byte[]> salesReportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId) {
        byte[] data = reportService.generateSalesReportPdf(startDate, endDate, branchId);
        return buildPdfResponse(data, "sales_report_" + startDate + "_" + endDate + ".pdf");
    }

    // ==========================================
    // INVENTORY REPORTS
    // ==========================================

    @GetMapping("/inventory/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Download inventory report as Excel")
    public ResponseEntity<byte[]> inventoryReportExcel(@RequestParam(required = false) Long branchId) {
        byte[] data = reportService.generateInventoryReportExcel(branchId);
        return buildExcelResponse(data, "inventory_report_" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/inventory/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Download inventory report as PDF")
    public ResponseEntity<byte[]> inventoryReportPdf(@RequestParam(required = false) Long branchId) {
        byte[] data = reportService.generateInventoryReportPdf(branchId);
        return buildPdfResponse(data, "inventory_report_" + LocalDate.now() + ".pdf");
    }

    // ==========================================
    // PROFIT & LOSS REPORTS
    // ==========================================

    @GetMapping("/profit-loss/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Download profit & loss report as Excel")
    public ResponseEntity<byte[]> profitLossReportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId) {
        byte[] data = reportService.generateProfitLossReportExcel(startDate, endDate, branchId);
        return buildExcelResponse(data, "profit_loss_" + startDate + "_" + endDate + ".xlsx");
    }

    @GetMapping("/profit-loss/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Download profit & loss report as PDF")
    public ResponseEntity<byte[]> profitLossReportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId) {
        byte[] data = reportService.generateProfitLossReportPdf(startDate, endDate, branchId);
        return buildPdfResponse(data, "profit_loss_" + startDate + "_" + endDate + ".pdf");
    }

    // ==========================================
    // PURCHASE ORDER REPORTS
    // ==========================================

    @GetMapping("/purchase-orders/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Download purchase order report as Excel")
    public ResponseEntity<byte[]> purchaseOrderReportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        byte[] data = reportService.generatePurchaseOrderReportExcel(startDate, endDate);
        return buildExcelResponse(data, "purchase_orders_" + startDate + "_" + endDate + ".xlsx");
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private ResponseEntity<byte[]> buildExcelResponse(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}
