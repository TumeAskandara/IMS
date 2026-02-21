package com.ims.dto.expense;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseRequest {

    @NotBlank(message = "Category is required")
    private String category; // 1. RENT, ELECTRICITY, etc.

    @NotBlank(message = "Description is required")
    @Size(max = 200)
    private String description; // 2. "Paid monthly rent for Main Branch"

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01")
    private BigDecimal amount; // 3. 50000.00

    @NotNull(message = "Date is required")
    private LocalDate expenseDate; // 4. 2026-02-16

    @Size(max = 1000)
    private String notes; // Optional: Any additional info
}