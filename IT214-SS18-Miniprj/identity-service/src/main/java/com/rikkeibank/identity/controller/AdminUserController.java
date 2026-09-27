package com.rikkeibank.identity.controller;

import com.rikkeibank.identity.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;

    @PostMapping("/{username}/force-logout")
    public ResponseEntity<Map<String, Object>> forceLogout(
            @PathVariable String username,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        // Validate role if forwarded through Gateway
        if (userRole != null && !userRole.contains("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "FORBIDDEN", "message", "Chỉ Quản trị viên (ADMIN) mới có quyền ép buộc đăng xuất người dùng"));
        }

        authService.forceLogoutUser(username);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Đã ép buộc đăng xuất thành công tài khoản: " + username,
                "targetUser", username
        ));
    }
}
