package com.ims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "currencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3)
    private String code; // ISO 4217 code (USD, EUR, ZAR, etc.)

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 5)
    private String symbol;

    @Column(name = "exchange_rate", precision = 15, scale = 6, nullable = false)
    private BigDecimal exchangeRate; // Rate relative to base currency

    @Column(name = "is_base", nullable = false)
    @Builder.Default
    private Boolean isBase = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}