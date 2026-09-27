package com.rikkeibank.account.controller;

import com.rikkeibank.account.dto.*;
import com.rikkeibank.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountDto> getAccountByNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    @GetMapping("/customer/{cif}")
    public ResponseEntity<List<AccountDto>> getAccountsByCustomerCif(@PathVariable String cif) {
        return ResponseEntity.ok(accountService.getAccountsByCustomerCif(cif));
    }

    @PostMapping
    public ResponseEntity<AccountDto> openAccount(@Valid @RequestBody AccountDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.openAccount(dto));
    }

    // Saga Step 1: Debit
    @PostMapping("/debit")
    public ResponseEntity<BalanceOperationResponse> debit(@Valid @RequestBody DebitRequest request) {
        return ResponseEntity.ok(accountService.debit(request));
    }

    // Saga Step 2: Credit
    @PostMapping("/credit")
    public ResponseEntity<BalanceOperationResponse> credit(
            @Valid @RequestBody CreditRequest request,
            @RequestHeader(value = "X-Simulate-Error", required = false) String simulateErrorHeader) {

        if ("true".equalsIgnoreCase(simulateErrorHeader)) {
            request.setSimulateError(true);
        }

        return ResponseEntity.ok(accountService.credit(request));
    }

    // Saga Rollback: Compensate Debit
    @PostMapping("/compensate-debit")
    public ResponseEntity<BalanceOperationResponse> compensateDebit(@Valid @RequestBody CompensateDebitRequest request) {
        return ResponseEntity.ok(accountService.compensateDebit(request));
    }
}
