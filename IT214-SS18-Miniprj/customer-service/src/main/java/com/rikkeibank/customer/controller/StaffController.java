package com.rikkeibank.customer.controller;

import com.rikkeibank.customer.dto.StaffDto;
import com.rikkeibank.customer.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staffs")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public ResponseEntity<List<StaffDto>> getAllStaffs() {
        return ResponseEntity.ok(staffService.getAllStaffs());
    }

    @GetMapping("/{code}")
    public ResponseEntity<StaffDto> getStaffByCode(@PathVariable String code) {
        return ResponseEntity.ok(staffService.getStaffByCode(code));
    }

    @PostMapping
    public ResponseEntity<StaffDto> createStaff(@Valid @RequestBody StaffDto dto) {
        StaffDto created = staffService.createStaff(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{code}")
    public ResponseEntity<StaffDto> updateStaff(@PathVariable String code, @Valid @RequestBody StaffDto dto) {
        StaffDto updated = staffService.updateStaff(code, dto);
        return ResponseEntity.ok(updated);
    }
}
