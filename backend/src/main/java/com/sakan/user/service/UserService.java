package com.sakan.user.service;

import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    Page<User> search(String q, RoleName role, Boolean enabled, Pageable pageable);
    User grantRole(long userId, RoleName role, long actorId);
    User revokeRole(long userId, RoleName role);
    long countByRole(RoleName role);
}
