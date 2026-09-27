package com.rikkeibank.account.controller;

import com.rikkeibank.account.dto.AccountTypeDto;
import com.rikkeibank.account.service.AccountTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/account-types")
@RequiredArgsConstructor
public class AccountTypeController {

    private final AccountTypeService accountTypeService;

    @GetMapping
    public ResponseEntity<List<AccountTypeDto>> getAllAccountTypes() {
        return ResponseEntity.ok(accountTypeService.getAllAccountTypes());
    }

    @GetMapping("/{code}")
    public ResponseEntity<AccountTypeDto> getAccountTypeByCode(@PathVariable String code) {
        return ResponseEntity.ok(accountTypeService.getAccountTypeByCode(code));
    }

    @PostMapping
    public ResponseEntity<AccountTypeDto> createAccountType(@Valid @RequestBody AccountTypeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountTypeService.createAccountType(dto));
    }

    @PutMapping("/{code}")
    public ResponseEntity<AccountTypeDto> updateAccountType(@PathVariable String code, @Valid @RequestBody AccountTypeDto dto) {
        return ResponseEntity.ok(accountTypeService.updateAccountType(code, dto));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Map<String, String>> deleteAccountType(@PathVariable String code) {
        accountTypeService.deleteAccountType(code);
        return ResponseEntity.ok(Map.of("message", "Đã vô hiệu hóa loại tài khoản: " + code));
    }
}
