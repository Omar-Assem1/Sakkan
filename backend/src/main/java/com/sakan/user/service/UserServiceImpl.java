package com.sakan.user.service;

import com.sakan.user.domain.AuthProvider;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.repository.RoleRepository;
import com.sakan.user.repository.UserRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional
    public User createLocalUser(String username, String email, String passwordHash, String phone, RoleName roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleName));
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .phone(phone)
                .authProvider(AuthProvider.LOCAL)
                .enabled(true)
                .roles(new HashSet<>(Set.of(role)))
                .build();
        return userRepository.save(user);
    }
    @Override
    public boolean existsSeededAdmin() {
        return userRepository.existsBySeededAdminTrue();
    }

    @Override
    @Transactional
    public User createSeededAdmin(String username, String email, String passwordHash) {
        Role admin = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + RoleName.ADMIN));
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .authProvider(AuthProvider.LOCAL)
                .seededAdmin(true)
                .enabled(true)
                .roles(new HashSet<>(Set.of(admin)))
                .build();
        return userRepository.save(user);
    }

    @Override
    public Page<User> search(String q, RoleName role, Boolean enabled, Pageable pageable) {
        String normalized = q == null || q.isBlank() ? null : q.trim()
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return userRepository.search(normalized, role, enabled, pageable);
    }

    @Override
    @Transactional
    public User grantRole(long userId, RoleName role, long actorId) {
        userRepository.grantRole(userId, role.name(), actorId);
        return userRepository.findById(userId).orElseThrow();
    }

    @Override
    @Transactional
    public User revokeRole(long userId, RoleName role) {
        userRepository.revokeRole(userId, role.name());
        return userRepository.findById(userId).orElseThrow();
    }

    @Override
    public long countByRole(RoleName role) {
        return userRepository.countByRole(role);
    }
}
