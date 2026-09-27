package com.rikkeibank.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTypeDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "Mã loại tài khoản không được để trống")
    private String typeCode;

    @NotBlank(message = "Tên loại tài khoản không được để trống")
    private String typeName;

    private String description;
    private BigDecimal interestRate;
    private BigDecimal minBalance;
    private String status;
}
