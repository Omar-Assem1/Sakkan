package com.sakan.user.service;

import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import java.util.Optional;

/** Other modules use this interface, never UserRepository. */
public interface UserService {

    boolean existsByUsername(String username);
    boolean existsSeededAdmin();

    /** Creates the single hardcoded admin (ADMIN role, is_seeded_admin = true). The caller passes an already hashed password. */
    User createSeededAdmin(String username, String email, String passwordHash);
    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);

    User createLocalUser(String username, String email, String passwordHash, String phone, RoleName role);
}