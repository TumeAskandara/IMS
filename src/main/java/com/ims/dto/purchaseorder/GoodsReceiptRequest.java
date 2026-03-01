package com.ims.dto.purchaseorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptRequest {

    @NotNull(message = "At least one item is required")
    @Size(min = 1, message = "At least one item is required")
    @Valid
    private List<GoodsReceiptItemRequest> items;

    private String notes;
}
