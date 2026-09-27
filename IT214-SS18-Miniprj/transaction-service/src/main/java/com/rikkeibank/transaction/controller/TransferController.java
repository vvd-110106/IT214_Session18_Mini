package com.rikkeibank.transaction.controller;

import com.rikkeibank.transaction.dto.TransferRequest;
import com.rikkeibank.transaction.dto.TransferResponse;
import com.rikkeibank.transaction.service.TransactionSagaOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransactionSagaOrchestrator sagaOrchestrator;

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-Customer-Cif", required = false) String customerCif,
            @RequestHeader(value = "X-Simulate-Error", required = false) String simulateErrorHeader) {

        if ("true".equalsIgnoreCase(simulateErrorHeader)) {
            request.setSimulateError(true);
        }

        TransferResponse response = sagaOrchestrator.executeTransferSaga(request, username, userRole, customerCif);
        return ResponseEntity.ok(response);
    }
}
