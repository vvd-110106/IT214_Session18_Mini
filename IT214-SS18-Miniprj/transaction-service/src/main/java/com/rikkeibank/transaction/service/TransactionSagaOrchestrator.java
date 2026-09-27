package com.rikkeibank.transaction.service;

import com.rikkeibank.transaction.client.AccountServiceClient;
import com.rikkeibank.transaction.client.dto.*;
import com.rikkeibank.transaction.dto.TransferRequest;
import com.rikkeibank.transaction.dto.TransferResponse;
import com.rikkeibank.transaction.entity.TransactionRecord;
import com.rikkeibank.transaction.event.TransferEvent;
import com.rikkeibank.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionSagaOrchestrator {

    private final AccountServiceClient accountServiceClient;
    private final TransactionRepository transactionRepository;
    private final KafkaProducerService kafkaProducerService;

    @CircuitBreaker(name = "accountService", fallbackMethod = "transferCircuitBreakerFallback")
    public TransferResponse executeTransferSaga(TransferRequest request, String performedBy, String userRole, String customerCif) {
        String txnCode = "TXN-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();
        log.info("========== [SAGA ORCHESTRATOR BẮT ĐẦU] Mã GD: {}, Từ tk: {} -> Đến tk: {}, Số tiền: {} VND ==========",
                txnCode, request.getFromAccount(), request.getToAccount(), request.getAmount());

        // Khởi tạo Transaction Record
        TransactionRecord record = TransactionRecord.builder()
                .transactionCode(txnCode)
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .currency("VND")
                .description(request.getDescription())
                .status("PENDING")
                .step("INITIAL")
                .performedBy(performedBy != null ? performedBy : "SYSTEM")
                .customerCif(customerCif)
                .tellerCode(userRole != null && userRole.contains("TELLER") ? performedBy : null)
                .createdAt(LocalDateTime.now())
                .build();

        record = transactionRepository.save(record);

        // ==========================================
        // BƯỚC 1: SAGA DEBIT (Trừ tiền tài khoản nguồn)
        // ==========================================
        try {
            log.info("--> [SAGA STEP 1] Đang thực hiện trừ tiền tài khoản nguồn: {}", request.getFromAccount());
            DebitRequest debitReq = DebitRequest.builder()
                    .accountNumber(request.getFromAccount())
                    .amount(request.getAmount())
                    .transactionCode(txnCode)
                    .build();

            BalanceOperationResponse debitRes = accountServiceClient.debit(debitReq);
            log.info("--> [SAGA STEP 1 THÀNH CÔNG] Đã trừ {} VND từ tk {}", debitRes.getAmount(), debitRes.getAccountNumber());
            record.setStep("DEBITED");
            transactionRepository.save(record);

        } catch (Exception e) {
            log.error("--> [SAGA STEP 1 THẤT BẠI] Lỗi khi trừ tiền tài khoản nguồn: {}", e.getMessage());
            record.setStatus("FAILED");
            record.setFailureReason("Lỗi trừ tiền tài khoản nguồn: " + e.getMessage());
            record.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(record);

            kafkaProducerService.sendTransferEvent(TransferEvent.builder()
                    .transactionCode(txnCode)
                    .fromAccount(request.getFromAccount())
                    .toAccount(request.getToAccount())
                    .amount(request.getAmount())
                    .currency("VND")
                    .status("FAILED")
                    .message("Trừ tiền thất bại: " + e.getMessage())
                    .performedBy(record.getPerformedBy())
                    .build());

            throw new IllegalArgumentException("Không thể thực hiện giao dịch: " + e.getMessage());
        }

        // ==========================================
        // BƯỚC 2: SAGA CREDIT (Cộng tiền tài khoản đích)
        // ==========================================
        try {
            log.info("--> [SAGA STEP 2] Đang thực hiện cộng tiền tài khoản đích: {}", request.getToAccount());
            CreditRequest creditReq = CreditRequest.builder()
                    .accountNumber(request.getToAccount())
                    .amount(request.getAmount())
                    .transactionCode(txnCode)
                    .simulateError(request.isSimulateError())
                    .build();

            String simulateErrorHeader = request.isSimulateError() ? "true" : null;
            BalanceOperationResponse creditRes = accountServiceClient.credit(creditReq, simulateErrorHeader);
            log.info("--> [SAGA STEP 2 THÀNH CÔNG] Đã cộng {} VND vào tk {}", creditRes.getAmount(), creditRes.getAccountNumber());

            // Toàn bộ Saga thành công
            record.setStatus("SUCCESS");
            record.setStep("CREDITED");
            record.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(record);

            log.info("========== [SAGA HOÀN TẤT THÀNH CÔNG] Mã GD: {} ==========", txnCode);

            // Bắn sự kiện hoàn thành lên Kafka
            kafkaProducerService.sendTransferEvent(TransferEvent.builder()
                    .transactionCode(txnCode)
                    .fromAccount(request.getFromAccount())
                    .toAccount(request.getToAccount())
                    .amount(request.getAmount())
                    .currency("VND")
                    .status("SUCCESS")
                    .message("Chuyển khoản thành công")
                    .performedBy(record.getPerformedBy())
                    .build());

            return TransferResponse.builder()
                    .transactionCode(txnCode)
                    .fromAccount(request.getFromAccount())
                    .toAccount(request.getToAccount())
                    .amount(request.getAmount())
                    .currency("VND")
                    .description(request.getDescription())
                    .status("SUCCESS")
                    .message("Chuyển khoản thành công")
                    .performedBy(record.getPerformedBy())
                    .createdAt(record.getCreatedAt())
                    .completedAt(record.getCompletedAt())
                    .build();

        } catch (Exception e) {
            log.error("--> [SAGA STEP 2 THẤT BẠI] Lỗi khi cộng tiền tài khoản đích: {}. BẮT ĐẦU BƯỚC BÙ TRỪ (COMPENSATING ROLLBACK)!", e.getMessage());

            // =======================================================
            // BƯỚC BÙ TRỪ: SAGA COMPENSATING ROLLBACK (Hoàn lại tiền nguồn)
            // =======================================================
            try {
                CompensateDebitRequest compensateReq = CompensateDebitRequest.builder()
                        .accountNumber(request.getFromAccount())
                        .amount(request.getAmount())
                        .transactionCode(txnCode)
                        .reason("Rollback: Không thể ghi có tài khoản đích (" + e.getMessage() + ")")
                        .build();

                accountServiceClient.compensateDebit(compensateReq);
                log.info("--> [SAGA COMPENSATING HOÀN TẤT] Đã hoàn trả {} VND về tài khoản nguồn {}", request.getAmount(), request.getFromAccount());

                record.setStatus("COMPENSATED");
                record.setStep("ROLLED_BACK");
                record.setFailureReason("Lỗi tài khoản đích: " + e.getMessage() + ". Đã bù trừ hoàn tiền tk nguồn.");
                record.setCompletedAt(LocalDateTime.now());
                transactionRepository.save(record);

                // Bắn sự kiện bù trừ hoàn tiền lên Kafka
                kafkaProducerService.sendTransferEvent(TransferEvent.builder()
                        .transactionCode(txnCode)
                        .fromAccount(request.getFromAccount())
                        .toAccount(request.getToAccount())
                        .amount(request.getAmount())
                        .currency("VND")
                        .status("COMPENSATED")
                        .message("Giao dịch bị hoàn tiền do lỗi tại tài khoản nhận")
                        .performedBy(record.getPerformedBy())
                        .build());

                return TransferResponse.builder()
                        .transactionCode(txnCode)
                        .fromAccount(request.getFromAccount())
                        .toAccount(request.getToAccount())
                        .amount(request.getAmount())
                        .currency("VND")
                        .description(request.getDescription())
                        .status("COMPENSATED")
                        .message("Giao dịch thất bại tại tài khoản đích. Cơ chế Saga đã hoàn trả tiền về tài khoản nguồn an toàn.")
                        .performedBy(record.getPerformedBy())
                        .createdAt(record.getCreatedAt())
                        .completedAt(record.getCompletedAt())
                        .build();

            } catch (Exception exCompensate) {
                log.error("--> [SAGA COMPENSATING CRITICAL ERROR] Không thể hoàn tiền tự động: {}", exCompensate.getMessage());
                record.setStatus("FAILED");
                record.setFailureReason("Lỗi nghiêm trọng khi rollback bù trừ: " + exCompensate.getMessage());
                transactionRepository.save(record);
                throw new IllegalStateException("Lỗi trong quá trình bù trừ giao dịch: " + exCompensate.getMessage());
            }
        }
    }

    // Fallback method cho Resilience4j Circuit Breaker
    public TransferResponse transferCircuitBreakerFallback(TransferRequest request, String performedBy, String userRole, String customerCif, Throwable t) {
        log.error("--> [RESILIENCE4J CIRCUIT BREAKER ACTIVATED] Dịch vụ account-service không phản hồi hoặc Circuit Breaker đang OPEN: {}", t.getMessage());

        return TransferResponse.builder()
                .transactionCode("CB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .currency("VND")
                .description(request.getDescription())
                .status("FAILED")
                .message("Dịch vụ tài khoản (account-service) đang tạm thời gián đoạn. Circuit Breaker đã chặn giao dịch an toàn, không có tài khoản nào bị trừ tiền.")
                .performedBy(performedBy != null ? performedBy : "SYSTEM")
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
    }
}
