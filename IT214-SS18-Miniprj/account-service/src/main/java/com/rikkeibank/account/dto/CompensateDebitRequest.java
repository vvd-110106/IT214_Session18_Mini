package com.rikkeibank.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensateDebitRequest {
    @NotBlank(message = "Số tài khoản không được để trống")
    private String accountNumber;

    @NotNull(message = "Số tiền hoàn không được để trống")
    @DecimalMin(value = "1000", message = "Số tiền hoàn tối thiểu là 1,000 VND")
    private BigDecimal amount;

    private String transactionCode;
    private String reason;
}
