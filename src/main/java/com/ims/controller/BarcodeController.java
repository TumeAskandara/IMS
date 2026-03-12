package com.ims.controller;

import com.ims.service.BarcodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/barcodes")
@RequiredArgsConstructor
@Tag(name = "Barcode & QR Code", description = "Generate barcodes and QR codes for products")
@SecurityRequirement(name = "bearerAuth")
public class BarcodeController {

    private final BarcodeService barcodeService;

    @GetMapping(value = "/product/{productId}/barcode", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Generate barcode for product")
    public ResponseEntity<byte[]> generateProductBarcode(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "100") int height) {
        byte[] image = barcodeService.generateProductBarcode(productId, width, height);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=barcode-" + productId + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @GetMapping(value = "/product/{productId}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Generate QR code for product")
    public ResponseEntity<byte[]> generateProductQRCode(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "250") int width,
            @RequestParam(defaultValue = "250") int height) {
        byte[] image = barcodeService.generateProductQRCode(productId, width, height);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=qrcode-" + productId + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @GetMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Generate barcode from arbitrary text")
    public ResponseEntity<byte[]> generateBarcode(
            @RequestParam String content,
            @RequestParam(defaultValue = "barcode") String type,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "100") int height) {
        byte[] image;
        if ("qrcode".equalsIgnoreCase(type)) {
            image = barcodeService.generateQRCode(content, width, height);
        } else {
            image = barcodeService.generateBarcode(content, width, height);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }
}