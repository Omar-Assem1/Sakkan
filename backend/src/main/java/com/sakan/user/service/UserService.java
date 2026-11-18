package com.sakan.user.service;

import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import java.util.Optional;

/** Other modules use this interface, never UserRepository. */
public interface UserService {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);

    User createLocalUser(String username, String email, String passwordHash, String phone, RoleName role);
}