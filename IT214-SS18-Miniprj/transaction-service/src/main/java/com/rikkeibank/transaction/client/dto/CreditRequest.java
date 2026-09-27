package com.rikkeibank.transaction.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditRequest {
    private String accountNumber;
    private BigDecimal amount;
    private String transactionCode;
    private boolean simulateError;
}
