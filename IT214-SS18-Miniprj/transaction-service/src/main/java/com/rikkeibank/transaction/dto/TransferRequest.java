package com.rikkeibank.transaction.dto;

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
public class TransferRequest {

    @NotBlank(message = "Tài khoản nguồn không được để trống")
    private String fromAccount;

    @NotBlank(message = "Tài khoản đích không được để trống")
    private String toAccount;

    @NotNull(message = "Số tiền chuyển không được để trống")
    @DecimalMin(value = "1000", message = "Số tiền chuyển tối thiểu là 1,000 VND")
    private BigDecimal amount;

    private String description;

    // Simulation flag for testing Saga Rollback
    private boolean simulateError;
}
