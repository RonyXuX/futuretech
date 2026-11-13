package com.futuretech.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.futuretech.entity.User;
import com.futuretech.entity.enums.Role;
import com.futuretech.repository.UserRepository;

/**
 * Creates the admin from ADMIN_EMAIL / ADMIN_PASSWORD, so its password is never in SQL or git.
 * Does nothing if they are unset or the account already exists.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminAccountInitializer(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${futuretech.admin.email:}") String adminEmail,
                                   @Value("${futuretech.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.trim().toLowerCase(java.util.Locale.ROOT);  // logins compare lowercase
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set: no admin account created.");
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setFirstName("Store");
        admin.setLastName("Admin");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Admin account created for {}", adminEmail);
    }
}
