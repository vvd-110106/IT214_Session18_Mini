package com.rikkeibank.identity.service;

import com.rikkeibank.identity.dto.AuthResponse;
import com.rikkeibank.identity.dto.LoginRequest;
import com.rikkeibank.identity.dto.RefreshTokenRequest;
import com.rikkeibank.identity.dto.RegisterRequest;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.repository.UserRepository;
import com.rikkeibank.identity.security.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' đã tồn tại trong hệ thống");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' đã được sử dụng");
        }

        String role = (request.getRole() != null && !request.getRole().isBlank()) 
                ? request.getRole().toUpperCase() 
                : "CUSTOMER";

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(role)
                .cif(request.getCif())
                .staffCode(request.getStaffCode())
                .status("ACTIVE")
                .build();

        user = userRepository.save(user);

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProvider.getAccessTokenExpirationSeconds())
                .username(user.getUsername())
                .role(user.getRole())
                .cif(user.getCif())
                .staffCode(user.getStaffCode())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("Tài khoản của bạn đang bị khóa hoặc ngưng hoạt động");
        }

        // If user was previously force-logged out, we can remove the blacklist flag on fresh valid login
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("blacklist:user:" + user.getUsername());
            } catch (Exception e) {
                log.warn("Redis delete error: {}", e.getMessage());
            }
        }

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProvider.getAccessTokenExpirationSeconds())
                .username(user.getUsername())
                .role(user.getRole())
                .cif(user.getCif())
                .staffCode(user.getStaffCode())
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!jwtProvider.validateToken(token)) {
            throw new IllegalArgumentException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String username = jwtProvider.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng sở hữu token"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("Tài khoản đang bị khóa");
        }

        String newAccessToken = jwtProvider.generateAccessToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtProvider.getAccessTokenExpirationSeconds())
                .username(user.getUsername())
                .role(user.getRole())
                .cif(user.getCif())
                .staffCode(user.getStaffCode())
                .build();
    }

    public void forceLogoutUser(String targetUsername) {
        User user = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + targetUsername));

        if (redisTemplate != null) {
            try {
                // Set blacklist flag for this user in Redis for 24 hours
                String key = "blacklist:user:" + targetUsername;
                redisTemplate.opsForValue().set(key, "REVOKED", 24, TimeUnit.HOURS);
                log.info("Quản trị viên đã ép buộc đăng xuất (Force Logout) tài khoản: {}", targetUsername);
            } catch (Exception e) {
                log.warn("Could not save force logout to Redis: {}", e.getMessage());
            }
        }
    }

    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ") && redisTemplate != null) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtProvider.getClaims(token);
                String jti = claims.getId();
                if (jti != null) {
                    redisTemplate.opsForValue().set("blacklist:token:" + jti, "LOGGED_OUT", 2, TimeUnit.HOURS);
                }
            } catch (Exception e) {
                log.warn("Error invalidating token: {}", e.getMessage());
            }
        }
    }
}
