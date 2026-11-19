package com.sakan.user.repository;

import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);

    boolean existsBySeededAdminTrue();

    boolean existsByEmail(String email);

    @Query("select distinct u from User u left join u.roles r where "
            + "(:q is null or lower(u.username) like lower(concat('%', :q, '%')) escape '!' "
            + "or lower(u.email) like lower(concat('%', :q, '%')) escape '!') and "
            + "(:role is null or r.name = :role) and (:enabled is null or u.enabled = :enabled)")
    Page<User> search(@Param("q") String q, @Param("role") RoleName role,
                      @Param("enabled") Boolean enabled, Pageable pageable);

    @Query("select count(distinct u) from User u join u.roles r where r.name = :role")
    long countByRole(@Param("role") RoleName role);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "insert into user_roles (user_id, role_id, granted_by, granted_at) "
            + "select :userId, r.id, :actorId, now() from roles r where r.name = :role "
            + "on conflict (user_id, role_id) do nothing", nativeQuery = true)
    int grantRole(@Param("userId") long userId, @Param("role") String role, @Param("actorId") long actorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "delete from user_roles where user_id = :userId and role_id = "
            + "(select id from roles where name = :role)", nativeQuery = true)
    int revokeRole(@Param("userId") long userId, @Param("role") String role);
}
