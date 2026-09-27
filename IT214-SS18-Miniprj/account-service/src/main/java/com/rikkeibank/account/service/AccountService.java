package com.rikkeibank.account.service;

import com.rikkeibank.account.dto.*;
import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.entity.AccountType;
import com.rikkeibank.account.repository.AccountRepository;
import com.rikkeibank.account.repository.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;

    public List<AccountDto> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<AccountDto> getAccountsByCustomerCif(String cif) {
        return accountRepository.findByCustomerCif(cif).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "accounts", key = "#accountNumber")
    public AccountDto getAccountByNumber(String accountNumber) {
        log.info("--> [REDIS CACHE MISS] Đang đọc thông tin tài khoản {} từ Cơ sở dữ liệu...", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản ngân hàng số: " + accountNumber));
        return mapToDto(account);
    }

    @Transactional
    public AccountDto openAccount(AccountDto dto) {
        if (dto.getAccountNumber() == null || dto.getAccountNumber().isBlank()) {
            dto.setAccountNumber("100" + (1000000 + new Random().nextInt(9000000)));
        }

        if (accountRepository.existsByAccountNumber(dto.getAccountNumber())) {
            throw new IllegalArgumentException("Số tài khoản '" + dto.getAccountNumber() + "' đã tồn tại");
        }

        String typeCode = (dto.getAccountTypeCode() != null) ? dto.getAccountTypeCode().toUpperCase() : "CHECKING";
        AccountType type = accountTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("Loại tài khoản không hợp lệ: " + typeCode));

        BigDecimal initialBalance = dto.getBalance() != null ? dto.getBalance() : type.getMinBalance();

        Account account = Account.builder()
                .accountNumber(dto.getAccountNumber())
                .customerCif(dto.getCustomerCif())
                .accountTypeCode(type.getTypeCode())
                .balance(initialBalance)
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "VND")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return mapToDto(accountRepository.save(account));
    }

    // Saga Step 1: Debit Source Account
    @Transactional
    @CacheEvict(value = "accounts", key = "#request.accountNumber")
    public BalanceOperationResponse debit(DebitRequest request) {
        log.info("--> [SAGA STEP 1 - DEBIT] Yêu cầu trừ tiền tk {}: số tiền {} VND, TxnCode: {}",
                request.getAccountNumber(), request.getAmount(), request.getTransactionCode());

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản nguồn không tồn tại: " + request.getAccountNumber()));

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalStateException("Tài khoản nguồn đang bị khóa hoặc đóng băng");
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("--> Số dư không đủ! Hiện có: {}, Cần trừ: {}", account.getBalance(), request.getAmount());
            throw new IllegalArgumentException("Số dư khả dụng không đủ để thực hiện giao dịch (Hiện có: " + account.getBalance() + " VND)");
        }

        BigDecimal prevBalance = account.getBalance();
        BigDecimal newBalance = prevBalance.subtract(request.getAmount());
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("--> Trừ tiền thành công! Số dư cũ: {}, Số dư mới: {}", prevBalance, newBalance);

        return BalanceOperationResponse.builder()
                .accountNumber(account.getAccountNumber())
                .amount(request.getAmount())
                .previousBalance(prevBalance)
                .currentBalance(newBalance)
                .transactionCode(request.getTransactionCode())
                .status("SUCCESS")
                .message("Trừ tiền thành công")
                .build();
    }

    // Saga Step 2: Credit Destination Account
    @Transactional
    @CacheEvict(value = "accounts", key = "#request.accountNumber")
    public BalanceOperationResponse credit(CreditRequest request) {
        log.info("--> [SAGA STEP 2 - CREDIT] Yêu cầu cộng tiền tk {}: số tiền {} VND, TxnCode: {}",
                request.getAccountNumber(), request.getAmount(), request.getTransactionCode());

        // Kịch bản cố tình gây lỗi kiểm thử Saga Rollback
        if (request.isSimulateError()) {
            log.error("--> [SAGA SIMULATION ERROR] Kích hoạt giả lập lỗi tại bước Credit tk đích theo yêu cầu kiểm thử!");
            throw new IllegalStateException("SIMULATED_CREDIT_ERROR: Lỗi hệ thống khi ghi có tài khoản đích (Minh chứng Rollback)");
        }

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản đích không tồn tại: " + request.getAccountNumber()));

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalStateException("Tài khoản đích đang bị khóa hoặc đóng băng");
        }

        BigDecimal prevBalance = account.getBalance();
        BigDecimal newBalance = prevBalance.add(request.getAmount());
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("--> Cộng tiền thành công! Số dư cũ: {}, Số dư mới: {}", prevBalance, newBalance);

        return BalanceOperationResponse.builder()
                .accountNumber(account.getAccountNumber())
                .amount(request.getAmount())
                .previousBalance(prevBalance)
                .currentBalance(newBalance)
                .transactionCode(request.getTransactionCode())
                .status("SUCCESS")
                .message("Cộng tiền thành công")
                .build();
    }

    // Saga Rollback / Compensating Step: Hoàn tiền lại cho tk nguồn khi Credit thất bại
    @Transactional
    @CacheEvict(value = "accounts", key = "#request.accountNumber")
    public BalanceOperationResponse compensateDebit(CompensateDebitRequest request) {
        log.info("--> [SAGA COMPENSATING - ROLLBACK] Hoàn trả tiền vào tk {}: số tiền {} VND, Lý do: {}",
                request.getAccountNumber(), request.getAmount(), request.getReason());

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản để hoàn tiền: " + request.getAccountNumber()));

        BigDecimal prevBalance = account.getBalance();
        BigDecimal newBalance = prevBalance.add(request.getAmount());
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("--> Đã hoàn trả bù trừ thành công! Số dư đã phục hồi về: {}", newBalance);

        return BalanceOperationResponse.builder()
                .accountNumber(account.getAccountNumber())
                .amount(request.getAmount())
                .previousBalance(prevBalance)
                .currentBalance(newBalance)
                .transactionCode(request.getTransactionCode())
                .status("SUCCESS")
                .message("Đã hoàn tiền bù trừ thành công (Compensating completed)")
                .build();
    }

    private AccountDto mapToDto(Account entity) {
        return AccountDto.builder()
                .id(entity.getId())
                .accountNumber(entity.getAccountNumber())
                .customerCif(entity.getCustomerCif())
                .accountTypeCode(entity.getAccountTypeCode())
                .balance(entity.getBalance())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
