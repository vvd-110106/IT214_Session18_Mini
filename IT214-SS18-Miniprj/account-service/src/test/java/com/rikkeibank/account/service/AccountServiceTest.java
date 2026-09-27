package com.rikkeibank.account.service;

import com.rikkeibank.account.dto.BalanceOperationResponse;
import com.rikkeibank.account.dto.CompensateDebitRequest;
import com.rikkeibank.account.dto.CreditRequest;
import com.rikkeibank.account.dto.DebitRequest;
import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.repository.AccountRepository;
import com.rikkeibank.account.repository.AccountTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTypeRepository accountTypeRepository;

    @InjectMocks
    private AccountService accountService;

    private Account sourceAccount;
    private Account destAccount;

    @BeforeEach
    void setUp() {
        sourceAccount = Account.builder()
                .id(1L)
                .accountNumber("1001234567")
                .customerCif("CIF001")
                .balance(BigDecimal.valueOf(10000000)) // 10M
                .status("ACTIVE")
                .currency("VND")
                .build();

        destAccount = Account.builder()
                .id(2L)
                .accountNumber("1009876543")
                .customerCif("CIF002")
                .balance(BigDecimal.valueOf(5000000)) // 5M
                .status("ACTIVE")
                .currency("VND")
                .build();
    }

    @Test
    @DisplayName("Saga Step 1: Trừ tiền thành công khi số dư đủ")
    void debit_Success() {
        when(accountRepository.findByAccountNumber("1001234567")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sourceAccount);

        DebitRequest request = DebitRequest.builder()
                .accountNumber("1001234567")
                .amount(BigDecimal.valueOf(2000000))
                .transactionCode("TXN-TEST-001")
                .build();

        BalanceOperationResponse response = accountService.debit(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(BigDecimal.valueOf(8000000), response.getCurrentBalance());
        verify(accountRepository, times(1)).save(sourceAccount);
    }

    @Test
    @DisplayName("Saga Step 1: Ném ngoại lệ khi số dư không đủ để trừ")
    void debit_InsufficientBalance_ThrowsException() {
        when(accountRepository.findByAccountNumber("1001234567")).thenReturn(Optional.of(sourceAccount));

        DebitRequest request = DebitRequest.builder()
                .accountNumber("1001234567")
                .amount(BigDecimal.valueOf(20000000)) // 20M > 10M
                .transactionCode("TXN-TEST-002")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> accountService.debit(request));
        assertTrue(ex.getMessage().contains("Số dư khả dụng không đủ"));
        verify(accountRepository, never()).save(sourceAccount);
    }

    @Test
    @DisplayName("Saga Step 2: Cộng tiền thành công vào tài khoản đích")
    void credit_Success() {
        when(accountRepository.findByAccountNumber("1009876543")).thenReturn(Optional.of(destAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(destAccount);

        CreditRequest request = CreditRequest.builder()
                .accountNumber("1009876543")
                .amount(BigDecimal.valueOf(2000000))
                .transactionCode("TXN-TEST-001")
                .simulateError(false)
                .build();

        BalanceOperationResponse response = accountService.credit(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(BigDecimal.valueOf(7000000), response.getCurrentBalance());
        verify(accountRepository, times(1)).save(destAccount);
    }

    @Test
    @DisplayName("Saga Step 2: Kích hoạt lỗi giả lập khi cờ simulateError = true")
    void credit_SimulateError_ThrowsException() {
        CreditRequest request = CreditRequest.builder()
                .accountNumber("1009876543")
                .amount(BigDecimal.valueOf(2000000))
                .transactionCode("TXN-TEST-003")
                .simulateError(true)
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> accountService.credit(request));
        assertTrue(ex.getMessage().contains("SIMULATED_CREDIT_ERROR"));
    }

    @Test
    @DisplayName("Saga Rollback: Hoàn tiền bù trừ thành công về số dư ban đầu")
    void compensateDebit_Success() {
        // Giả sử sau khi trừ tiền tài khoản chỉ còn 8M
        sourceAccount.setBalance(BigDecimal.valueOf(8000000));
        when(accountRepository.findByAccountNumber("1001234567")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sourceAccount);

        CompensateDebitRequest request = CompensateDebitRequest.builder()
                .accountNumber("1001234567")
                .amount(BigDecimal.valueOf(2000000))
                .transactionCode("TXN-TEST-003")
                .reason("Compensating Rollback test")
                .build();

        BalanceOperationResponse response = accountService.compensateDebit(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(BigDecimal.valueOf(10000000), response.getCurrentBalance()); // phục hồi lại 10M
        verify(accountRepository, times(1)).save(sourceAccount);
    }
}
