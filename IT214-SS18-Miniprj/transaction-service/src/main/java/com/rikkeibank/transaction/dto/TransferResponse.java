package com.rikkeibank.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private String transactionCode;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String status; // SUCCESS, FAILED, COMPENSATED
    private String message;
    private String performedBy;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
