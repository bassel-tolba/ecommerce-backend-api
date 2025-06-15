package com.ecommerce.app.config;

import java.util.Set; // <-- ADD THIS IMPORT

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder; // <-- ADD THIS IMPORT
import org.springframework.stereotype.Component;

import com.ecommerce.app.model.Role;
import com.ecommerce.app.model.User;
import com.ecommerce.app.repository.RoleRepository;
import com.ecommerce.app.repository.UserRepository; // <-- ADD THIS IMPORT

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository; // <-- INJECT USER REPOSITORY
    private final PasswordEncoder passwordEncoder; // <-- INJECT PASSWORD ENCODER

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking for initial data...");

        // --- Create Roles (as before) ---
        Role userRole = null;
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            Role newUserRole = new Role();
            newUserRole.setName("ROLE_USER");
            newUserRole.setDescription("Default role for all registered users.");
            userRole = roleRepository.save(newUserRole);
            log.info("Created ROLE_USER");
        } else {
            userRole = roleRepository.findByName("ROLE_USER").get();
        }

        Role adminRole = null;
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            Role newAdminRole = new Role();
            newAdminRole.setName("ROLE_ADMIN");
            newAdminRole.setDescription("Administrator role with full permissions.");
            adminRole = roleRepository.save(newAdminRole);
            log.info("Created ROLE_ADMIN");
        } else {
            adminRole = roleRepository.findByName("ROLE_ADMIN").get();
        }

        // --- Create a Default Admin User ---
        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            User admin = new User();
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setEmail("admin@example.com");
            // Use a secure password and hash it!
            admin.setPasswordHash(passwordEncoder.encode("adminpassword"));
            admin.setActive(true);

            // Assign both ROLE_USER and ROLE_ADMIN
            admin.setRoles(Set.of(userRole, adminRole));

            userRepository.save(admin);
            log.info("Created default admin user with email 'admin@example.com'");
        }

        log.info("Initial data check complete.");
    }
}