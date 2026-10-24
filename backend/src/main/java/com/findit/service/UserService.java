package com.findit.service;

import com.findit.dto.UserRegistrationDto;
import com.findit.exception.InvalidStateTransitionException;
import com.findit.exception.ResourceNotFoundException;
import com.findit.exception.UnauthorizedException;
import com.findit.model.User;
import com.findit.model.enums.Role;
import com.findit.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(UserRegistrationDto dto) {
        if (!dto.isPasswordMatching()) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (userRepository.existsByEmailIgnoreCase(dto.getEmail().trim())) {
            throw new IllegalArgumentException("An account with email " + dto.getEmail() + " already exists");
        }

        User user = new User(
                dto.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getFullName().trim(),
                dto.getPhone() != null ? dto.getPhone().trim() : null,
                Role.ROLE_USER
        );

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public User updateProfile(User user, String fullName, String phone) {
        user.setFullName(fullName.trim());
        user.setPhone(phone != null ? phone.trim() : null);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // Admin operations
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(int page, int size) {
        return userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    @Transactional
    public void toggleUserStatus(Long targetUserId, User adminUser) {
        if (targetUserId.equals(adminUser.getId())) {
            throw new InvalidStateTransitionException("Administrators cannot disable their own account");
        }

        User targetUser = findById(targetUserId);
        targetUser.setEnabled(!targetUser.isEnabled());
        userRepository.save(targetUser);
    }

    @Transactional
    public void toggleUserRole(Long targetUserId, User adminUser) {
        if (targetUserId.equals(adminUser.getId())) {
            throw new InvalidStateTransitionException("Administrators cannot change their own role");
        }

        User targetUser = findById(targetUserId);
        if (targetUser.getRole() == Role.ROLE_ADMIN) {
            long adminCount = userRepository.countByRole(Role.ROLE_ADMIN);
            if (adminCount <= 1) {
                throw new InvalidStateTransitionException("Cannot demote the last remaining administrator");
            }
            targetUser.setRole(Role.ROLE_USER);
        } else {
            targetUser.setRole(Role.ROLE_ADMIN);
        }
        userRepository.save(targetUser);
    }
}
