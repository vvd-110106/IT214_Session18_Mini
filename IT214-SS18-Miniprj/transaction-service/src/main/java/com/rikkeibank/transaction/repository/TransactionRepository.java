package com.rikkeibank.transaction.repository;

import com.rikkeibank.transaction.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {
    Optional<TransactionRecord> findByTransactionCode(String transactionCode);
    List<TransactionRecord> findByTellerCodeAndCreatedAtBetween(String tellerCode, LocalDateTime start, LocalDateTime end);
    List<TransactionRecord> findByPerformedBy(String performedBy);
    List<TransactionRecord> findByFromAccountOrToAccount(String fromAccount, String toAccount);
    List<TransactionRecord> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
