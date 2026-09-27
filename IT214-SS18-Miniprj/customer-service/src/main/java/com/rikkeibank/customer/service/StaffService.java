package com.rikkeibank.customer.service;

import com.rikkeibank.customer.dto.StaffDto;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;

    public List<StaffDto> getAllStaffs() {
        return staffRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public StaffDto getStaffByCode(String code) {
        Staff staff = staffRepository.findByStaffCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với mã: " + code));
        return mapToDto(staff);
    }

    @Transactional
    public StaffDto createStaff(StaffDto dto) {
        if (staffRepository.existsByStaffCode(dto.getStaffCode())) {
            throw new IllegalArgumentException("Mã nhân viên '" + dto.getStaffCode() + "' đã tồn tại");
        }

        Staff staff = Staff.builder()
                .staffCode(dto.getStaffCode())
                .fullName(dto.getFullName())
                .role(dto.getRole().toUpperCase())
                .department(dto.getDepartment())
                .branchCode(dto.getBranchCode())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        return mapToDto(staffRepository.save(staff));
    }

    @Transactional
    public StaffDto updateStaff(String code, StaffDto dto) {
        Staff staff = staffRepository.findByStaffCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với mã: " + code));

        staff.setFullName(dto.getFullName());
        staff.setRole(dto.getRole().toUpperCase());
        staff.setDepartment(dto.getDepartment());
        staff.setBranchCode(dto.getBranchCode());
        if (dto.getStatus() != null) {
            staff.setStatus(dto.getStatus());
        }

        return mapToDto(staffRepository.save(staff));
    }

    private StaffDto mapToDto(Staff entity) {
        return StaffDto.builder()
                .id(entity.getId())
                .staffCode(entity.getStaffCode())
                .fullName(entity.getFullName())
                .role(entity.getRole())
                .department(entity.getDepartment())
                .branchCode(entity.getBranchCode())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
