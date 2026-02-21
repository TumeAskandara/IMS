package com.ims.dto.expense;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDTO {
    private Long id;
    private String branchName;
    private String category;
    private String description;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String notes;
    private String recordedByName;
    private LocalDateTime createdAt;
}