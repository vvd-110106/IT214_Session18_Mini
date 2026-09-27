package com.rikkeibank.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "account_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountType implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String typeCode; // e.g. CHECKING, SAVINGS

    @Column(nullable = false, length = 100)
    private String typeName; // e.g. Tài khoản thanh toán, Tài khoản tiết kiệm

    @Column(length = 255)
    private String description;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal minBalance = BigDecimal.valueOf(50000); // 50k VND

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
