package com.example.Repetition_7.dev;

import com.example.Repetition_7.entity.UserEntity;
import com.example.Repetition_7.entity.roles.UserRole;
import com.example.Repetition_7.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevAdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.dev.admin.username}")
    private String adminUsername;
    @Value("${app.dev.admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (adminUsername == null || adminUsername.isBlank()) {
            throw new IllegalStateException("app.dev.admin.username is blank");
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("app.dev.admin.password is blank");
        }

        String normalized = adminUsername.trim().toLowerCase();
        String encodedPassword = passwordEncoder.encode(adminPassword);

        var existing = userRepository.findByUsername(normalized);

        if(existing.isPresent()) {
            UserEntity user = existing.get();
            user.setRole(UserRole.ADMIN);
            user.setPasswordHash(encodedPassword);
            userRepository.save(user);
            log.info("ADMIN exists");

        } else {
            UserEntity user = new UserEntity();
            user.setUsername(normalized);
            user.setPasswordHash(encodedPassword);
            user.setRole(UserRole.ADMIN);
            userRepository.save(user);
            log.info("ADMIN created");
        }
        userRepository.flush();
    }
}
