package com.learnmate.config;

import com.learnmate.model.Role;
import com.learnmate.model.User;
import com.learnmate.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class DefaultAdminInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DefaultAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-admin.enabled:true}")
    private boolean adminSeedEnabled;

    @Value("${app.seed-admin.username:admin}")
    private String adminUsername;

    @Value("${app.seed-admin.password:Admin@12345}")
    private String adminPassword;

    @Value("${app.seed-admin.email:admin@learnmate.com}")
    private String adminEmail;

    @Value("${app.seed-admin.name:System Administrator}")
    private String adminName;

    @Value("${app.seed-admin.contact:+1-555-0100}")
    private String adminContact;

    public DefaultAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!adminSeedEnabled) {
            log.info("Default admin seeding disabled (app.seed-admin.enabled=false). Skipping.");
            return;
        }

        userRepository.findByUsername(adminUsername).ifPresentOrElse(existing -> {
            boolean updated = false;

            if (!Role.ADMIN.equals(existing.getRole())) {
                existing.setRole(Role.ADMIN);
                updated = true;
            }

            if (!Objects.equals(existing.getEmail(), adminEmail)) {
                existing.setEmail(adminEmail);
                updated = true;
            }

            if (!Objects.equals(existing.getName(), adminName)) {
                existing.setName(adminName);
                updated = true;
            }

            if (!Objects.equals(existing.getContact(), adminContact)) {
                existing.setContact(adminContact);
                updated = true;
            }

            String currentPassword = existing.getPassword();
            if (currentPassword == null || !passwordEncoder.matches(adminPassword, currentPassword)) {
                existing.setPassword(passwordEncoder.encode(adminPassword));
                updated = true;
            }

            if (!existing.isActive()) {
                existing.setActive(true);
                updated = true;
            }

            if (updated) {
                userRepository.save(existing);
                log.info("Existing admin '{}' synchronized with configured defaults.", adminUsername);
            } else {
                log.info("Admin '{}' already configured. No changes applied.", adminUsername);
            }
        }, () -> {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setEmail(adminEmail);
            admin.setName(adminName);
            admin.setContact(adminContact);
            admin.setRole(Role.ADMIN);
            admin.setActive(true);

            userRepository.save(admin);
            log.info("Default admin '{}' created successfully.", adminUsername);
        });
    }
}
