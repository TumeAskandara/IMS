package com.ims.service;

import com.ims.entity.Category;
import com.ims.entity.Product;
import com.ims.exception.BadRequestException;
import com.ims.repository.CategoryRepository;
import com.ims.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkImportService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Map<String, Object> importProductsFromExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        List<Product> imported = new ArrayList<>();
        int rowNum = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            if (rows.hasNext()) {
                rows.next();
                rowNum++;
            }

            while (rows.hasNext()) {
                Row row = rows.next();
                rowNum++;
                try {
                    Product product = parseProductRow(row);
                    if (product != null) {
                        imported.add(productRepository.save(product));
                    }
                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalProcessed", rowNum - 1);
        result.put("successCount", imported.size());
        result.put("errorCount", errors.size());
        result.put("errors", errors);
        log.info("Bulk import complete: {} imported, {} errors", imported.size(), errors.size());
        return result;
    }

    private Product parseProductRow(Row row) {
        String sku = getCellStringValue(row, 0);
        String name = getCellStringValue(row, 1);
        if (sku == null || sku.isBlank()) {
            throw new BadRequestException("SKU is required");
        }
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        if (productRepository.existsBySku(sku)) {
            throw new BadRequestException("SKU '" + sku + "' already exists");
        }

        String description = getCellStringValue(row, 2);
        String brand = getCellStringValue(row, 3);
        String model = getCellStringValue(row, 4);
        String barcode = getCellStringValue(row, 5);
        BigDecimal unitPrice = getCellBigDecimalValue(row, 6);
        BigDecimal costPrice = getCellBigDecimalValue(row, 7);
        String unit = getCellStringValue(row, 8);
        Integer reorderLevel = getCellIntValue(row, 9);
        Integer minimumStock = getCellIntValue(row, 10);
        String categoryName = getCellStringValue(row, 11);

        if (barcode != null && !barcode.isBlank() && productRepository.existsByBarcode(barcode)) {
            throw new BadRequestException("Barcode '" + barcode + "' already exists");
        }

        Category category = null;
        if (categoryName != null && !categoryName.isBlank()) {
            category = categoryRepository.findByNameIgnoreCase(categoryName).orElse(null);
        }

        return Product.builder()
                .sku(sku)
                .name(name)
                .description(description)
                .brand(brand)
                .model(model)
                .barcode(barcode)
                .unitPrice(unitPrice)
                .costPrice(costPrice)
                .unit(unit)
                .reorderLevel(reorderLevel)
                .minimumStock(minimumStock)
                .category(category)
                .isActive(true)
                .build();
    }

    public byte[] generateImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            String[] columns = {"SKU*", "Name*", "Description", "Brand", "Model", "Barcode",
                    "Unit Price", "Cost Price", "Unit", "Reorder Level", "Minimum Stock", "Category Name"};

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("SKU-001");
            sample.createCell(1).setCellValue("Sample Product");
            sample.createCell(2).setCellValue("Description here");
            sample.createCell(3).setCellValue("BrandName");
            sample.createCell(4).setCellValue("ModelX");
            sample.createCell(5).setCellValue("1234567890");
            sample.createCell(6).setCellValue(99.99);
            sample.createCell(7).setCellValue(50.00);
            sample.createCell(8).setCellValue("PCS");
            sample.createCell(9).setCellValue(10);
            sample.createCell(10).setCellValue(5);
            sample.createCell(11).setCellValue("Category Name");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate template", e);
        }
    }

    private String getCellStringValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return null;
    }

    private BigDecimal getCellBigDecimalValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue());
        if (cell.getCellType() == CellType.STRING) {
            try { return new BigDecimal(cell.getStringCellValue().trim()); }
            catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private Integer getCellIntValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        if (cell.getCellType() == CellType.STRING) {
            try { return Integer.parseInt(cell.getStringCellValue().trim()); }
            catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}