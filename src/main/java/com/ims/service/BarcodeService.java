package com.ims.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ims.entity.Product;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BarcodeService {

    private final ProductRepository productRepository;

    public byte[] generateBarcode(String content, int width, int height) {
        try {
            Code128Writer writer = new Code128Writer();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate barcode: {}", e.getMessage());
            throw new RuntimeException("Failed to generate barcode", e);
        }
    }

    public byte[] generateQRCode(String content, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate QR code: {}", e.getMessage());
            throw new RuntimeException("Failed to generate QR code", e);
        } catch (WriterException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateProductBarcode(Long productId, int width, int height) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        String content = product.getBarcode() != null ? product.getBarcode() : product.getSku();
        return generateBarcode(content, width, height);
    }

    @Transactional(readOnly = true)
    public byte[] generateProductQRCode(Long productId, int width, int height) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        String content = String.format("SKU:%s|NAME:%s|PRICE:%s",
                product.getSku(), product.getName(),
                product.getUnitPrice() != null ? product.getUnitPrice().toPlainString() : "N/A");
        return generateQRCode(content, width, height);
    }
}