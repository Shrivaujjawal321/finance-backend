package com.finance.config;

import com.finance.entity.User;
import com.finance.enums.Role;
import com.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@finance.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Admin")
                    .role(Role.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);

            User analyst = User.builder()
                    .username("analyst")
                    .email("analyst@finance.com")
                    .password(passwordEncoder.encode("analyst123"))
                    .fullName("Finance Analyst")
                    .role(Role.ANALYST)
                    .active(true)
                    .build();
            userRepository.save(analyst);

            User viewer = User.builder()
                    .username("viewer")
                    .email("viewer@finance.com")
                    .password(passwordEncoder.encode("viewer123"))
                    .fullName("Dashboard Viewer")
                    .role(Role.VIEWER)
                    .active(true)
                    .build();
            userRepository.save(viewer);

            log.info("Seeded default users: admin, analyst, viewer");
        }
    }
}
