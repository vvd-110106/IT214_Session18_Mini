package com.rikkeibank.account.service;

import com.rikkeibank.account.dto.AccountTypeDto;
import com.rikkeibank.account.entity.AccountType;
import com.rikkeibank.account.repository.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountTypeService {

    private final AccountTypeRepository accountTypeRepository;

    @Cacheable(value = "accountTypes")
    public List<AccountTypeDto> getAllAccountTypes() {
        log.info("--> [REDIS CACHE MISS] Đang truy vấn danh mục loại tài khoản từ Cơ sở dữ liệu...");
        return accountTypeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public AccountTypeDto getAccountTypeByCode(String code) {
        AccountType type = accountTypeRepository.findByTypeCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại tài khoản mã: " + code));
        return mapToDto(type);
    }

    @Transactional
    @CacheEvict(value = "accountTypes", allEntries = true)
    public AccountTypeDto createAccountType(AccountTypeDto dto) {
        log.info("--> [REDIS CACHE EVICT] Thêm loại tài khoản mới, làm mới bộ nhớ đệm 'accountTypes'");
        if (accountTypeRepository.existsByTypeCode(dto.getTypeCode())) {
            throw new IllegalArgumentException("Mã loại tài khoản '" + dto.getTypeCode() + "' đã tồn tại");
        }

        AccountType type = AccountType.builder()
                .typeCode(dto.getTypeCode().toUpperCase())
                .typeName(dto.getTypeName())
                .description(dto.getDescription())
                .interestRate(dto.getInterestRate())
                .minBalance(dto.getMinBalance())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .build();

        return mapToDto(accountTypeRepository.save(type));
    }

    @Transactional
    @CacheEvict(value = "accountTypes", allEntries = true)
    public AccountTypeDto updateAccountType(String code, AccountTypeDto dto) {
        log.info("--> [REDIS CACHE EVICT] Cập nhật loại tài khoản {}, làm mới bộ nhớ đệm 'accountTypes'", code);
        AccountType type = accountTypeRepository.findByTypeCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại tài khoản mã: " + code));

        type.setTypeName(dto.getTypeName());
        type.setDescription(dto.getDescription());
        if (dto.getInterestRate() != null) type.setInterestRate(dto.getInterestRate());
        if (dto.getMinBalance() != null) type.setMinBalance(dto.getMinBalance());
        if (dto.getStatus() != null) type.setStatus(dto.getStatus());

        return mapToDto(accountTypeRepository.save(type));
    }

    @Transactional
    @CacheEvict(value = "accountTypes", allEntries = true)
    public void deleteAccountType(String code) {
        log.info("--> [REDIS CACHE EVICT] Xóa loại tài khoản {}, làm mới bộ nhớ đệm 'accountTypes'", code);
        AccountType type = accountTypeRepository.findByTypeCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại tài khoản mã: " + code));
        type.setStatus("INACTIVE");
        accountTypeRepository.save(type);
    }

    private AccountTypeDto mapToDto(AccountType entity) {
        return AccountTypeDto.builder()
                .id(entity.getId())
                .typeCode(entity.getTypeCode())
                .typeName(entity.getTypeName())
                .description(entity.getDescription())
                .interestRate(entity.getInterestRate())
                .minBalance(entity.getMinBalance())
                .status(entity.getStatus())
                .build();
    }
}
