package com.rikkeibank.customer.controller;

import com.rikkeibank.customer.dto.CustomerDto;
import com.rikkeibank.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{cif}")
    public ResponseEntity<CustomerDto> getCustomerByCif(@PathVariable String cif) {
        return ResponseEntity.ok(customerService.getCustomerByCif(cif));
    }

    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CustomerDto dto) {
        CustomerDto created = customerService.createCustomer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{cif}")
    public ResponseEntity<CustomerDto> updateCustomer(@PathVariable String cif, @Valid @RequestBody CustomerDto dto) {
        CustomerDto updated = customerService.updateCustomer(cif, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{cif}")
    public ResponseEntity<Map<String, String>> deleteCustomer(@PathVariable String cif) {
        customerService.deleteCustomer(cif);
        return ResponseEntity.ok(Map.of("message", "Đã vô hiệu hóa khách hàng mã: " + cif));
    }
}
