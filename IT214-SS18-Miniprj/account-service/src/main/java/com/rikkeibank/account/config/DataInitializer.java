package com.rikkeibank.account.config;

import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.entity.AccountType;
import com.rikkeibank.account.repository.AccountRepository;
import com.rikkeibank.account.repository.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AccountTypeRepository accountTypeRepository;
    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        if (accountTypeRepository.count() == 0) {
            log.info("Khởi tạo danh mục Loại tài khoản mẫu cho RikkeiBank...");
            accountTypeRepository.saveAll(List.of(
                    AccountType.builder()
                            .typeCode("CHECKING")
                            .typeName("Tài khoản thanh toán đa năng")
                            .description("Tài khoản phục vụ thanh toán, chuyển khoản, chi tiêu thường nhật")
                            .interestRate(BigDecimal.valueOf(0.2))
                            .minBalance(BigDecimal.valueOf(50000))
                            .status("ACTIVE")
                            .build(),

                    AccountType.builder()
                            .typeCode("SAVINGS")
                            .typeName("Tài khoản tiết kiệm điện tử")
                            .description("Tài khoản hưởng lãi suất có kỳ hạn sinh lời cao")
                            .interestRate(BigDecimal.valueOf(5.8))
                            .minBalance(BigDecimal.valueOf(1000000))
                            .status("ACTIVE")
                            .build()
            ));
        }

        if (accountRepository.count() == 0) {
            log.info("Khởi tạo các tài khoản ngân hàng mẫu cho RikkeiBank...");
            accountRepository.saveAll(List.of(
                    Account.builder()
                            .accountNumber("1001234567")
                            .customerCif("CIF001")
                            .accountTypeCode("CHECKING")
                            .balance(BigDecimal.valueOf(50000000)) // 50 triệu VND
                            .currency("VND")
                            .status("ACTIVE")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build(),

                    Account.builder()
                            .accountNumber("1009876543")
                            .customerCif("CIF002")
                            .accountTypeCode("CHECKING")
                            .balance(BigDecimal.valueOf(10000000)) // 10 triệu VND
                            .currency("VND")
                            .status("ACTIVE")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build()
            ));
        }
    }
}
