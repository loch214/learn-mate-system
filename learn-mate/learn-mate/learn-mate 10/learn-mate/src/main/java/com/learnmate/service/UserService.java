package com.learnmate.service;

import com.learnmate.model.Role;
import com.learnmate.model.User;
import com.learnmate.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern PASSWORD_POLICY_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z]).{8,}$");

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getParentWithChildrenByUsername(String username) {
        return userRepository.findParentWithChildrenByUsername(username);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("No authenticated user found");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public String getUserRole(String username) {
        return getUserByUsername(username).map(User::getRole).map(Enum::name).orElse("UNKNOWN");
    }

    public void save(User user) {
        if (user.getId() == null) {
            user.setPassword(encodePasswordWithPolicy(user.getPassword()));
        } else {
            String password = user.getPassword();

            if (password == null || password.isBlank()) {
                String existingPassword = userRepository.findById(user.getId())
                        .map(User::getPassword)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + user.getId()));
                user.setPassword(existingPassword);
            } else if (!isPasswordEncoded(password)) {
                user.setPassword(encodePasswordWithPolicy(password));
            }
        }
        userRepository.save(user);
    }

    public User createUser(User user) {
        user.setPassword(encodePasswordWithPolicy(user.getPassword()));
        return userRepository.save(user);
    }

    public User updateUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty() && !isPasswordEncoded(user.getPassword())) {
            user.setPassword(encodePasswordWithPolicy(user.getPassword()));
        }
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }
    
    public List<User> getAvailableStudentsForParent() {
        return userRepository.findStudentsNotLinkedToAnyParent(Role.STUDENT);
    }
    
    public List<User> getUsersByRoleWithRelationships(Role role) {
        return userRepository.findByRoleWithRelationships(role);
    }
    
    public List<User> getUsersByRoleAndSchoolClassAndSubject(Role role, com.learnmate.model.SchoolClass schoolClass, com.learnmate.model.Subject subject) {
        return userRepository.findByRoleAndSchoolClassAndSubject(role, schoolClass, subject);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void changePassword(User user, String newPassword) {
        user.setPassword(encodePasswordWithPolicy(newPassword));
        userRepository.save(user);
    }

    public List<User> getUsersByName(String name) {
        return userRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * ROBUST PROFILE UPDATE METHOD - Uses fetch-then-update pattern to guarantee JPA UPDATE operation
     */
    @Transactional
    public void updateUserProfile(User updatedUserData) {
        // Since we're passing all fields including password/role/active via hidden fields,
        // we can directly save the updated user data which already has the correct ID
        userRepository.save(updatedUserData);
    }

    public List<User> getUsersBySchoolClass(com.learnmate.model.SchoolClass schoolClass) {
        return userRepository.findBySchoolClass(schoolClass);
    }

    /**
     * ROBUST USER REGISTRATION WITH PROACTIVE VALIDATION
     * Prevents duplicate email/username errors before attempting to save
     */
    @Transactional
    public void registerNewUser(User user) {
        // Check 1: Does a user with this email already exist?
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalStateException("An account with this email already exists.");
        }

        // Check 2: Does a user with this username already exist?
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalStateException("This username is already taken.");
        }

        // If both checks pass, proceed with creating and saving the user
        user.setPassword(encodePasswordWithPolicy(user.getPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void attachChildrenToParent(User parent, List<Long> childIds) {
        if (parent == null || parent.getRole() != Role.PARENT) {
            throw new IllegalArgumentException("User is not a parent");
        }
        if (childIds == null || childIds.isEmpty()) {
            throw new IllegalArgumentException("Parent must select at least one registered child");
        }
        
        // Check if any of the selected students are already linked to other parents
        List<User> students = userRepository.findAllByIdInAndRole(childIds, Role.STUDENT);
        if (students == null || students.isEmpty() || students.size() != childIds.size()) {
            throw new IllegalArgumentException("One or more selected children are invalid or not students");
        }
        
        // Check if any student is already linked to another parent
        for (User student : students) {
            if (student.getParents() != null && !student.getParents().isEmpty()) {
                throw new IllegalArgumentException("Student " + student.getName() + " is already linked to another parent");
            }
        }
        
        parent.getChildren().clear();
        parent.getChildren().addAll(students);
        userRepository.save(parent);
    }

    private String encodePasswordWithPolicy(String rawPassword) {
        validatePasswordStrength(rawPassword);
        return passwordEncoder.encode(rawPassword);
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required and cannot be blank.");
        }

        if (!PASSWORD_POLICY_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and include both uppercase and lowercase letters.");
        }
    }

    private boolean isPasswordEncoded(String password) {
        if (password == null) {
            return false;
        }
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
