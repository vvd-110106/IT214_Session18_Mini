package com.rikkeibank.transaction.service;

import com.rikkeibank.transaction.client.AccountServiceClient;
import com.rikkeibank.transaction.client.dto.BalanceOperationResponse;
import com.rikkeibank.transaction.client.dto.CompensateDebitRequest;
import com.rikkeibank.transaction.client.dto.CreditRequest;
import com.rikkeibank.transaction.client.dto.DebitRequest;
import com.rikkeibank.transaction.dto.TransferRequest;
import com.rikkeibank.transaction.dto.TransferResponse;
import com.rikkeibank.transaction.entity.TransactionRecord;
import com.rikkeibank.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionSagaOrchestratorTest {

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private TransactionSagaOrchestrator sagaOrchestrator;

    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        transferRequest = TransferRequest.builder()
                .fromAccount("1001234567")
                .toAccount("1009876543")
                .amount(BigDecimal.valueOf(1000000))
                .description("Chuyển tiền trả phí dịch vụ")
                .simulateError(false)
                .build();

        when(transactionRepository.save(any(TransactionRecord.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Saga Happy Path: Chuyển khoản thành công (Debit OK -> Credit OK -> SUCCESS)")
    void executeTransferSaga_Success() {
        BalanceOperationResponse mockDebitRes = BalanceOperationResponse.builder()
                .accountNumber("1001234567")
                .amount(BigDecimal.valueOf(1000000))
                .status("SUCCESS")
                .build();

        BalanceOperationResponse mockCreditRes = BalanceOperationResponse.builder()
                .accountNumber("1009876543")
                .amount(BigDecimal.valueOf(1000000))
                .status("SUCCESS")
                .build();

        when(accountServiceClient.debit(any(DebitRequest.class))).thenReturn(mockDebitRes);
        when(accountServiceClient.credit(any(CreditRequest.class), any())).thenReturn(mockCreditRes);

        TransferResponse response = sagaOrchestrator.executeTransferSaga(
                transferRequest, "customer1", "CUSTOMER", "CIF001"
        );

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("1001234567", response.getFromAccount());
        assertEquals("1009876543", response.getToAccount());

        verify(accountServiceClient, times(1)).debit(any(DebitRequest.class));
        verify(accountServiceClient, times(1)).credit(any(CreditRequest.class), any());
        verify(accountServiceClient, never()).compensateDebit(any(CompensateDebitRequest.class));
        verify(kafkaProducerService, times(1)).sendTransferEvent(any());
    }

    @Test
    @DisplayName("Saga Rollback Flow: Credit thất bại kích hoạt Compensating Rollback hoàn tiền (Debit OK -> Credit Error -> Compensate OK -> COMPENSATED)")
    void executeTransferSaga_CreditFails_TriggersRollback() {
        transferRequest.setSimulateError(true);

        BalanceOperationResponse mockDebitRes = BalanceOperationResponse.builder()
                .accountNumber("1001234567")
                .amount(BigDecimal.valueOf(1000000))
                .status("SUCCESS")
                .build();

        BalanceOperationResponse mockCompensateRes = BalanceOperationResponse.builder()
                .accountNumber("1001234567")
                .amount(BigDecimal.valueOf(1000000))
                .status("SUCCESS")
                .message("Đã hoàn tiền bù trừ thành công")
                .build();

        when(accountServiceClient.debit(any(DebitRequest.class))).thenReturn(mockDebitRes);
        when(accountServiceClient.credit(any(CreditRequest.class), any()))
                .thenThrow(new IllegalStateException("SIMULATED_CREDIT_ERROR: Lỗi giả lập tài khoản đích"));
        when(accountServiceClient.compensateDebit(any(CompensateDebitRequest.class))).thenReturn(mockCompensateRes);

        TransferResponse response = sagaOrchestrator.executeTransferSaga(
                transferRequest, "customer1", "CUSTOMER", "CIF001"
        );

        assertNotNull(response);
        assertEquals("COMPENSATED", response.getStatus());
        assertTrue(response.getMessage().contains("hoàn trả tiền"));

        verify(accountServiceClient, times(1)).debit(any(DebitRequest.class));
        verify(accountServiceClient, times(1)).credit(any(CreditRequest.class), any());
        verify(accountServiceClient, times(1)).compensateDebit(any(CompensateDebitRequest.class)); // Kích hoạt bù trừ!
        verify(kafkaProducerService, times(1)).sendTransferEvent(any());
    }
}
