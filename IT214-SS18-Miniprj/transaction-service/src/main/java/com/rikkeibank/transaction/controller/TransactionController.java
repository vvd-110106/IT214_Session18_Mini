package com.rikkeibank.transaction.controller;

import com.rikkeibank.transaction.entity.TransactionRecord;
import com.rikkeibank.transaction.service.TransactionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionQueryService transactionQueryService;

    // ADMIN: Xem toàn bộ giao dịch hệ thống
    @GetMapping
    public ResponseEntity<List<TransactionRecord>> getAllTransactions() {
        return ResponseEntity.ok(transactionQueryService.getAllTransactions());
    }

    // TELLER: Xem danh sách giao dịch trong ngày do chính mình thực hiện
    @GetMapping("/daily")
    public ResponseEntity<List<TransactionRecord>> getDailyTransactions(
            @RequestHeader(value = "X-User-Name", required = false, defaultValue = "teller1") String tellerUsername) {
        return ResponseEntity.ok(transactionQueryService.getDailyTransactionsForTeller(tellerUsername));
    }

    // CUSTOMER: Xem lịch sử giao dịch cá nhân
    @GetMapping("/my-history")
    public ResponseEntity<List<TransactionRecord>> getMyHistory(
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-Customer-Cif", required = false) String customerCif) {
        return ResponseEntity.ok(transactionQueryService.getMyTransactions(username, customerCif));
    }

    @GetMapping("/{code}")
    public ResponseEntity<TransactionRecord> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(transactionQueryService.getByTransactionCode(code));
    }
}
