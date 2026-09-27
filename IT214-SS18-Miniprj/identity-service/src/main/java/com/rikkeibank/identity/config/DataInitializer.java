package com.rikkeibank.identity.config;

import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Khởi tạo dữ liệu người dùng mẫu cho RikkeiBank Identity Service...");

            List<User> initialUsers = List.of(
                    User.builder()
                            .username("admin")
                            .password(passwordEncoder.encode("admin123"))
                            .email("admin@rikkeibank.com")
                            .role("ADMIN")
                            .staffCode("ADMIN001")
                            .status("ACTIVE")
                            .createdAt(LocalDateTime.now())
                            .build(),

                    User.builder()
                            .username("teller1")
                            .password(passwordEncoder.encode("teller123"))
                            .email("teller1@rikkeibank.com")
                            .role("TELLER")
                            .staffCode("STAFF001")
                            .status("ACTIVE")
                            .createdAt(LocalDateTime.now())
                            .build(),

                    User.builder()
                            .username("teller2")
                            .password(passwordEncoder.encode("teller123"))
                            .email("teller2@rikkeibank.com")
                            .role("TELLER")
                            .staffCode("STAFF002")
                            .status("ACTIVE")
                            .createdAt(LocalDateTime.now())
                            .build(),

                    User.builder()
                            .username("customer1")
                            .password(passwordEncoder.encode("cust123"))
                            .email("customer1@gmail.com")
                            .role("CUSTOMER")
                            .cif("CIF001")
                            .status("ACTIVE")
                            .createdAt(LocalDateTime.now())
                            .build(),

                    User.builder()
                            .username("customer2")
                            .password(passwordEncoder.encode("cust123"))
                            .email("customer2@gmail.com")
                            .role("CUSTOMER")
                            .cif("CIF002")
                            .status("ACTIVE")
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            userRepository.saveAll(initialUsers);
            log.info("Đã khởi tạo thành công {} tài khoản mẫu.", initialUsers.size());
        }
    }
}
