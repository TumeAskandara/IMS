package com.ims.controller;

import com.ims.dto.response.ApiResponse;
import com.ims.service.BulkImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/bulk-import")
@RequiredArgsConstructor
@Tag(name = "Bulk Import", description = "Import products from Excel files")
@SecurityRequirement(name = "bearerAuth")
public class BulkImportController {

    private final BulkImportService bulkImportService;

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Import products from Excel", description = "Upload an Excel file to bulk import products")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importProducts(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("File is empty"));
        }
        Map<String, Object> result = bulkImportService.importProductsFromExcel(file);
        return ResponseEntity.ok(ApiResponse.success("Import completed", result));
    }

    @GetMapping("/template/products")
    @Operation(summary = "Download import template", description = "Download Excel template for product import")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = bulkImportService.generateImportTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=product-import-template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(template);
    }
}