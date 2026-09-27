package com.rikkeibank.customer.service;

import com.rikkeibank.customer.dto.CustomerDto;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<CustomerDto> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public CustomerDto getCustomerByCif(String cif) {
        Customer customer = customerRepository.findByCustomerCif(cif)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng với mã CIF: " + cif));
        return mapToDto(customer);
    }

    @Transactional
    public CustomerDto createCustomer(CustomerDto dto) {
        if (dto.getCustomerCif() == null || dto.getCustomerCif().isBlank()) {
            dto.setCustomerCif("CIF" + (100000 + (int)(Math.random() * 900000)));
        }

        if (customerRepository.existsByCustomerCif(dto.getCustomerCif())) {
            throw new IllegalArgumentException("Mã CIF '" + dto.getCustomerCif() + "' đã tồn tại");
        }
        if (customerRepository.existsByIdentityCard(dto.getIdentityCard())) {
            throw new IllegalArgumentException("Số CCCD '" + dto.getIdentityCard() + "' đã tồn tại trong hệ thống");
        }

        Customer customer = Customer.builder()
                .customerCif(dto.getCustomerCif())
                .fullName(dto.getFullName())
                .identityCard(dto.getIdentityCard())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        return mapToDto(customerRepository.save(customer));
    }

    @Transactional
    public CustomerDto updateCustomer(String cif, CustomerDto dto) {
        Customer customer = customerRepository.findByCustomerCif(cif)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng với mã CIF: " + cif));

        customer.setFullName(dto.getFullName());
        customer.setPhone(dto.getPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
        if (dto.getStatus() != null) {
            customer.setStatus(dto.getStatus());
        }

        return mapToDto(customerRepository.save(customer));
    }

    @Transactional
    public void deleteCustomer(String cif) {
        Customer customer = customerRepository.findByCustomerCif(cif)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng với mã CIF: " + cif));
        customer.setStatus("INACTIVE");
        customerRepository.save(customer);
    }

    private CustomerDto mapToDto(Customer entity) {
        return CustomerDto.builder()
                .id(entity.getId())
                .customerCif(entity.getCustomerCif())
                .fullName(entity.getFullName())
                .identityCard(entity.getIdentityCard())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .address(entity.getAddress())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
