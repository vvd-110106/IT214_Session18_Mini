package com.rikkeibank.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDto {
    private Long id;

    @NotBlank(message = "Mã nhân viên không được để trống")
    private String staffCode;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @NotBlank(message = "Vai trò (TELLER/ADMIN) không được để trống")
    private String role;

    private String department;
    private String branchCode;
    private String status;
    private LocalDateTime createdAt;
}
