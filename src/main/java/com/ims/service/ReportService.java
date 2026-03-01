package com.ims.service;

import com.ims.entity.*;
import com.ims.repository.*;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final SaleRepository saleRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final ExpenseRepository expenseRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ==========================================
    // SALES REPORT
    // ==========================================

    public byte[] generateSalesReportExcel(LocalDate startDate, LocalDate endDate, Long branchId) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Sale> sales;
        if (branchId != null) {
            sales = saleRepository.findByBranchIdAndSaleDateBetween(branchId, start, end);
        } else {
            sales = saleRepository.findAll().stream()
                    .filter(s -> !s.getIsDeleted() && s.getSaleDate().isAfter(start) && s.getSaleDate().isBefore(end))
                    .toList();
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sales Report");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // Title
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("Sales Report: " + startDate + " to " + endDate);

            // Headers
            String[] headers = {"Invoice #", "Date", "Customer", "Branch", "Seller",
                    "Subtotal", "Tax", "Discount", "Total", "Paid", "Payment Method", "Status"};
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            BigDecimal grandTotal = BigDecimal.ZERO;
            for (Sale sale : sales) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sale.getInvoiceNumber());
                row.createCell(1).setCellValue(sale.getSaleDate().format(DATETIME_FMT));
                row.createCell(2).setCellValue(sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in");
                row.createCell(3).setCellValue(sale.getBranch().getName());
                row.createCell(4).setCellValue(sale.getSeller().getFullName());
                setCurrencyCell(row, 5, sale.getSubtotal(), currencyStyle);
                setCurrencyCell(row, 6, sale.getTaxAmount(), currencyStyle);
                setCurrencyCell(row, 7, sale.getDiscountAmount(), currencyStyle);
                setCurrencyCell(row, 8, sale.getTotalAmount(), currencyStyle);
                setCurrencyCell(row, 9, sale.getAmountPaid(), currencyStyle);
                row.createCell(10).setCellValue(sale.getPaymentMethod().name());
                row.createCell(11).setCellValue(sale.getStatus().name());
                grandTotal = grandTotal.add(sale.getTotalAmount());
            }

            // Summary row
            Row summaryRow = sheet.createRow(rowIdx + 1);
            summaryRow.createCell(7).setCellValue("Grand Total:");
            setCurrencyCell(summaryRow, 8, grandTotal, currencyStyle);

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating sales Excel report", e);
            throw new RuntimeException("Failed to generate sales report", e);
        }
    }

    public byte[] generateSalesReportPdf(LocalDate startDate, LocalDate endDate, Long branchId) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Sale> sales;
        if (branchId != null) {
            sales = saleRepository.findByBranchIdAndSaleDateBetween(branchId, start, end);
        } else {
            sales = saleRepository.findAll().stream()
                    .filter(s -> !s.getIsDeleted() && s.getSaleDate().isAfter(start) && s.getSaleDate().isBefore(end))
                    .toList();
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 8);

            document.add(new Paragraph("Sales Report", titleFont));
            document.add(new Paragraph("Period: " + startDate + " to " + endDate, cellFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            String[] headers = {"Invoice #", "Date", "Customer", "Branch", "Total", "Paid", "Payment", "Status"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(44, 62, 80));
                cell.setPadding(5);
                table.addCell(cell);
            }

            BigDecimal grandTotal = BigDecimal.ZERO;
            for (Sale sale : sales) {
                table.addCell(new Phrase(sale.getInvoiceNumber(), cellFont));
                table.addCell(new Phrase(sale.getSaleDate().format(DATETIME_FMT), cellFont));
                table.addCell(new Phrase(sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in", cellFont));
                table.addCell(new Phrase(sale.getBranch().getName(), cellFont));
                table.addCell(new Phrase(formatCurrency(sale.getTotalAmount()), cellFont));
                table.addCell(new Phrase(formatCurrency(sale.getAmountPaid()), cellFont));
                table.addCell(new Phrase(sale.getPaymentMethod().name(), cellFont));
                table.addCell(new Phrase(sale.getStatus().name(), cellFont));
                grandTotal = grandTotal.add(sale.getTotalAmount());
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Grand Total: " + formatCurrency(grandTotal),
                    new Font(Font.HELVETICA, 12, Font.BOLD)));
            document.add(new Paragraph("Total Sales: " + sales.size(), cellFont));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating sales PDF report", e);
            throw new RuntimeException("Failed to generate sales PDF report", e);
        }
    }

    // ==========================================
    // INVENTORY REPORT
    // ==========================================

    public byte[] generateInventoryReportExcel(Long branchId) {
        List<BranchInventory> inventories;
        if (branchId != null) {
            inventories = branchInventoryRepository.findByBranchId(branchId);
        } else {
            inventories = branchInventoryRepository.findAll();
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Inventory Report");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle lowStockStyle = workbook.createCellStyle();
            lowStockStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            lowStockStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle outOfStockStyle = workbook.createCellStyle();
            outOfStockStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            outOfStockStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("Inventory Report — " + LocalDate.now());

            String[] headers = {"SKU", "Product", "Category", "Branch", "Unit Price",
                    "Cost Price", "On Hand", "Reserved", "Available", "Value", "Status"};
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            BigDecimal totalValue = BigDecimal.ZERO;
            for (BranchInventory inv : inventories) {
                Product p = inv.getProduct();
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getSku());
                row.createCell(1).setCellValue(p.getName());
                row.createCell(2).setCellValue(p.getCategory() != null ? p.getCategory().getName() : "");
                row.createCell(3).setCellValue(inv.getBranch().getName());
                setCurrencyCell(row, 4, p.getUnitPrice(), currencyStyle);
                setCurrencyCell(row, 5, p.getCostPrice(), currencyStyle);
                row.createCell(6).setCellValue(inv.getQuantityOnHand());
                row.createCell(7).setCellValue(inv.getQuantityReserved());
                row.createCell(8).setCellValue(inv.getQuantityAvailable());

                BigDecimal value = p.getCostPrice() != null
                        ? p.getCostPrice().multiply(BigDecimal.valueOf(inv.getQuantityOnHand()))
                        : BigDecimal.ZERO;
                setCurrencyCell(row, 9, value, currencyStyle);
                totalValue = totalValue.add(value);

                String status;
                CellStyle statusStyle = null;
                if (inv.getQuantityOnHand() == 0) {
                    status = "OUT OF STOCK";
                    statusStyle = outOfStockStyle;
                } else if (p.getReorderLevel() != null && inv.getQuantityOnHand() < p.getReorderLevel()) {
                    status = "LOW STOCK";
                    statusStyle = lowStockStyle;
                } else {
                    status = "IN STOCK";
                }
                Cell statusCell = row.createCell(10);
                statusCell.setCellValue(status);
                if (statusStyle != null) statusCell.setCellStyle(statusStyle);
            }

            Row summaryRow = sheet.createRow(rowIdx + 1);
            summaryRow.createCell(8).setCellValue("Total Value:");
            setCurrencyCell(summaryRow, 9, totalValue, currencyStyle);

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating inventory Excel report", e);
            throw new RuntimeException("Failed to generate inventory report", e);
        }
    }

    public byte[] generateInventoryReportPdf(Long branchId) {
        List<BranchInventory> inventories;
        if (branchId != null) {
            inventories = branchInventoryRepository.findByBranchId(branchId);
        } else {
            inventories = branchInventoryRepository.findAll();
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 8);
            Font lowStockFont = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(204, 153, 0));
            Font outOfStockFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.RED);

            document.add(new Paragraph("Inventory Report", titleFont));
            document.add(new Paragraph("Date: " + LocalDate.now(), cellFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            String[] headers = {"SKU", "Product", "Branch", "Cost", "On Hand", "Available", "Value", "Status"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(44, 62, 80));
                cell.setPadding(5);
                table.addCell(cell);
            }

            BigDecimal totalValue = BigDecimal.ZERO;
            for (BranchInventory inv : inventories) {
                Product p = inv.getProduct();
                table.addCell(new Phrase(p.getSku(), cellFont));
                table.addCell(new Phrase(p.getName(), cellFont));
                table.addCell(new Phrase(inv.getBranch().getName(), cellFont));
                table.addCell(new Phrase(formatCurrency(p.getCostPrice()), cellFont));
                table.addCell(new Phrase(String.valueOf(inv.getQuantityOnHand()), cellFont));
                table.addCell(new Phrase(String.valueOf(inv.getQuantityAvailable()), cellFont));

                BigDecimal value = p.getCostPrice() != null
                        ? p.getCostPrice().multiply(BigDecimal.valueOf(inv.getQuantityOnHand()))
                        : BigDecimal.ZERO;
                table.addCell(new Phrase(formatCurrency(value), cellFont));
                totalValue = totalValue.add(value);

                Font statusFont;
                String status;
                if (inv.getQuantityOnHand() == 0) {
                    status = "OUT OF STOCK";
                    statusFont = outOfStockFont;
                } else if (p.getReorderLevel() != null && inv.getQuantityOnHand() < p.getReorderLevel()) {
                    status = "LOW STOCK";
                    statusFont = lowStockFont;
                } else {
                    status = "IN STOCK";
                    statusFont = cellFont;
                }
                table.addCell(new Phrase(status, statusFont));
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total Inventory Value: " + formatCurrency(totalValue),
                    new Font(Font.HELVETICA, 12, Font.BOLD)));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating inventory PDF report", e);
            throw new RuntimeException("Failed to generate inventory PDF report", e);
        }
    }

    // ==========================================
    // PROFIT & LOSS REPORT
    // ==========================================

    public byte[] generateProfitLossReportExcel(LocalDate startDate, LocalDate endDate, Long branchId) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        // Revenue
        BigDecimal revenue;
        if (branchId != null) {
            revenue = saleRepository.getNetSalesForBranch(branchId, start, end);
        } else {
            revenue = saleRepository.getNetSalesAllBranches(start, end);
        }
        revenue = revenue != null ? revenue : BigDecimal.ZERO;

        // COGS: sum of (cost_price * quantity) for all sold items
        List<Sale> sales;
        if (branchId != null) {
            sales = saleRepository.findByBranchIdAndSaleDateBetween(branchId, start, end);
        } else {
            sales = saleRepository.findAll().stream()
                    .filter(s -> !s.getIsDeleted() && s.getSaleDate().isAfter(start) && s.getSaleDate().isBefore(end))
                    .toList();
        }
        BigDecimal cogs = BigDecimal.ZERO;
        for (Sale sale : sales) {
            if (sale.getSaleItems() != null) {
                for (SaleItem item : sale.getSaleItems()) {
                    BigDecimal costPrice = item.getProduct().getCostPrice();
                    if (costPrice != null) {
                        cogs = cogs.add(costPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
                    }
                }
            }
        }

        BigDecimal grossProfit = revenue.subtract(cogs);

        // Expenses
        List<Expense> expenses;
        if (branchId != null) {
            expenses = expenseRepository.findByBranchAndDateRange(branchId, startDate, endDate);
        } else {
            expenses = expenseRepository.findAll().stream()
                    .filter(e -> !e.getIsDeleted()
                            && !e.getExpenseDate().isBefore(startDate)
                            && !e.getExpenseDate().isAfter(endDate))
                    .toList();
        }
        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netProfit = grossProfit.subtract(totalExpenses);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Profit & Loss");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle boldStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("Profit & Loss Report: " + startDate + " to " + endDate);

            int r = 2;
            addPLRow(sheet, r++, "Revenue (Net Sales)", revenue, currencyStyle, boldStyle);
            addPLRow(sheet, r++, "Cost of Goods Sold (COGS)", cogs, currencyStyle, null);
            addPLRow(sheet, r++, "Gross Profit", grossProfit, currencyStyle, boldStyle);
            r++;

            // Expense breakdown by category
            Row expHeader = sheet.createRow(r++);
            expHeader.createCell(0).setCellValue("Expenses by Category");
            expHeader.getCell(0).setCellStyle(boldStyle);

            var expenseByCategory = expenses.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Expense::getCategory,
                            java.util.stream.Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
            for (var entry : expenseByCategory.entrySet()) {
                addPLRow(sheet, r++, "  " + entry.getKey().name(), entry.getValue(), currencyStyle, null);
            }
            addPLRow(sheet, r++, "Total Expenses", totalExpenses, currencyStyle, boldStyle);
            r++;
            addPLRow(sheet, r, "Net Profit", netProfit, currencyStyle, boldStyle);

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating P&L Excel report", e);
            throw new RuntimeException("Failed to generate P&L report", e);
        }
    }

    public byte[] generateProfitLossReportPdf(LocalDate startDate, LocalDate endDate, Long branchId) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        BigDecimal revenue;
        if (branchId != null) {
            revenue = saleRepository.getNetSalesForBranch(branchId, start, end);
        } else {
            revenue = saleRepository.getNetSalesAllBranches(start, end);
        }
        revenue = revenue != null ? revenue : BigDecimal.ZERO;

        List<Sale> sales;
        if (branchId != null) {
            sales = saleRepository.findByBranchIdAndSaleDateBetween(branchId, start, end);
        } else {
            sales = saleRepository.findAll().stream()
                    .filter(s -> !s.getIsDeleted() && s.getSaleDate().isAfter(start) && s.getSaleDate().isBefore(end))
                    .toList();
        }
        BigDecimal cogs = BigDecimal.ZERO;
        for (Sale sale : sales) {
            if (sale.getSaleItems() != null) {
                for (SaleItem item : sale.getSaleItems()) {
                    if (item.getProduct().getCostPrice() != null) {
                        cogs = cogs.add(item.getProduct().getCostPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                    }
                }
            }
        }
        BigDecimal grossProfit = revenue.subtract(cogs);

        List<Expense> expenses;
        if (branchId != null) {
            expenses = expenseRepository.findByBranchAndDateRange(branchId, startDate, endDate);
        } else {
            expenses = expenseRepository.findAll().stream()
                    .filter(e -> !e.getIsDeleted()
                            && !e.getExpenseDate().isBefore(startDate)
                            && !e.getExpenseDate().isAfter(endDate))
                    .toList();
        }
        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netProfit = grossProfit.subtract(totalExpenses);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10);
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD);

            document.add(new Paragraph("Profit & Loss Statement", titleFont));
            document.add(new Paragraph("Period: " + startDate + " to " + endDate, normalFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(80);
            table.setWidths(new float[]{3, 1});

            addPdfPLRow(table, "Revenue (Net Sales)", formatCurrency(revenue), boldFont);
            addPdfPLRow(table, "Cost of Goods Sold", formatCurrency(cogs), normalFont);
            addPdfPLRow(table, "Gross Profit", formatCurrency(grossProfit), boldFont);
            addPdfPLRow(table, "", "", normalFont);

            var expenseByCategory = expenses.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Expense::getCategory,
                            java.util.stream.Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
            for (var entry : expenseByCategory.entrySet()) {
                addPdfPLRow(table, "  " + entry.getKey().name(), formatCurrency(entry.getValue()), normalFont);
            }
            addPdfPLRow(table, "Total Expenses", formatCurrency(totalExpenses), boldFont);
            addPdfPLRow(table, "", "", normalFont);
            addPdfPLRow(table, "NET PROFIT", formatCurrency(netProfit), sectionFont);

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating P&L PDF report", e);
            throw new RuntimeException("Failed to generate P&L PDF report", e);
        }
    }

    // ==========================================
    // PURCHASE ORDER REPORT
    // ==========================================

    public byte[] generatePurchaseOrderReportExcel(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<PurchaseOrder> orders = purchaseOrderRepository
                .findAllWithItemsByDateRange(start, end, Pageable.unpaged()).getContent();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Purchase Orders");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("Purchase Order Report: " + startDate + " to " + endDate);

            String[] headers = {"PO #", "Supplier", "Branch", "Order Date", "Expected Delivery",
                    "Status", "Subtotal", "Tax", "Shipping", "Total"};
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            BigDecimal grandTotal = BigDecimal.ZERO;
            for (PurchaseOrder po : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(po.getPoNumber());
                row.createCell(1).setCellValue(po.getSupplier().getName());
                row.createCell(2).setCellValue(po.getBranch().getName());
                row.createCell(3).setCellValue(po.getOrderDate().format(DATETIME_FMT));
                row.createCell(4).setCellValue(po.getExpectedDeliveryDate() != null
                        ? po.getExpectedDeliveryDate().format(DATE_FMT) : "");
                row.createCell(5).setCellValue(po.getStatus().name());
                setCurrencyCell(row, 6, po.getSubtotal(), currencyStyle);
                setCurrencyCell(row, 7, po.getTaxAmount(), currencyStyle);
                setCurrencyCell(row, 8, po.getShippingCost(), currencyStyle);
                setCurrencyCell(row, 9, po.getTotalAmount(), currencyStyle);
                grandTotal = grandTotal.add(po.getTotalAmount());
            }

            Row summaryRow = sheet.createRow(rowIdx + 1);
            summaryRow.createCell(8).setCellValue("Grand Total:");
            setCurrencyCell(summaryRow, 9, grandTotal, currencyStyle);

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PO Excel report", e);
            throw new RuntimeException("Failed to generate PO report", e);
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCurrencyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private void setCurrencyCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value.doubleValue() : 0);
        cell.setCellStyle(style);
    }

    private void addPLRow(Sheet sheet, int rowIdx, String label, BigDecimal value,
                          CellStyle currencyStyle, CellStyle labelStyle) {
        Row row = sheet.createRow(rowIdx);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        if (labelStyle != null) labelCell.setCellStyle(labelStyle);
        setCurrencyCell(row, 1, value, currencyStyle);
    }

    private void addPdfPLRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(0);
        labelCell.setPadding(4);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(0);
        valueCell.setPadding(4);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) return "0.00";
        return String.format("%,.2f", value);
    }
}
