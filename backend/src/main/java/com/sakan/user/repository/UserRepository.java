package com.sakan.user.repository;

import com.sakan.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);
    boolean existsBySeededAdminTrue();
    boolean existsByEmail(String email);
}
