package com.finance.service;

import com.finance.dto.request.ForgotPasswordRequest;
import com.finance.dto.request.LoginRequest;
import com.finance.dto.request.RegisterRequest;
import com.finance.dto.request.ResetPasswordRequest;
import com.finance.dto.response.AuthResponse;
import com.finance.dto.response.PasswordResetResponse;
import com.finance.entity.PasswordResetToken;
import com.finance.entity.User;
import com.finance.enums.Role;
import com.finance.exception.DuplicateResourceException;
import com.finance.repository.PasswordResetTokenRepository;
import com.finance.security.JwtService;
import com.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Public registration always assigns VIEWER role.
        // Role elevation requires an ADMIN to update via PUT /api/users/{id}.
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.VIEWER)
                .active(true)
                .build();

        userRepository.save(user);
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public PasswordResetResponse forgotPassword(ForgotPasswordRequest request) {
        // Always return a generic message to prevent email enumeration attacks.
        var userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return PasswordResetResponse.builder()
                    .message("If an account exists with this email, a password reset link has been sent.")
                    .build();
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        // In production, this token would be sent via email (JavaMailSender / SendGrid).
        // For local development, we log it to the server console to simulate email delivery.
        log.info("========== SIMULATED EMAIL ==========");
        log.info("To: {}", user.getEmail());
        log.info("Subject: Password Reset Request");
        log.info("Reset Token: {}", token);
        log.info("This token expires at: {}", resetToken.getExpiresAt());
        log.info("======================================");

        return PasswordResetResponse.builder()
                .message("If an account exists with this email, a password reset link has been sent.")
                .build();
    }

    @Transactional
    public PasswordResetResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or already used reset token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset token has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return PasswordResetResponse.builder()
                .message("Password has been reset successfully. You can now sign in.")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}
