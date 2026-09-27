package com.rikkeibank.transaction.service;

import com.rikkeibank.transaction.entity.TransactionRecord;
import com.rikkeibank.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionQueryService {

    private final TransactionRepository transactionRepository;

    public List<TransactionRecord> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public TransactionRecord getByTransactionCode(String code) {
        return transactionRepository.findByTransactionCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch mã: " + code));
    }

    // Nghiệp vụ TELLER: Xem danh sách giao dịch trong ngày do mình thực hiện
    public List<TransactionRecord> getDailyTransactionsForTeller(String tellerCode) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        return transactionRepository.findByTellerCodeAndCreatedAtBetween(tellerCode, startOfDay, endOfDay);
    }

    // Nghiệp vụ CUSTOMER: Chỉ xem lịch sử của chính mình
    public List<TransactionRecord> getMyTransactions(String username, String cif) {
        if (username != null && !username.isBlank()) {
            return transactionRepository.findByPerformedBy(username);
        }
        return List.of();
    }
}
