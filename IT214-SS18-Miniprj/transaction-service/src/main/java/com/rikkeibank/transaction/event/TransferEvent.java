package com.rikkeibank.transaction.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String transactionCode;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String status; // SUCCESS, FAILED, COMPENSATED
    private String message;
    private String performedBy;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
