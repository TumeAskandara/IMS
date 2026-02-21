package com.ims.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Integer quantityRequested;
    private Integer quantityShipped;
    private Integer quantityReceived;
    private Integer quantityDamaged;
    private String notes;
}