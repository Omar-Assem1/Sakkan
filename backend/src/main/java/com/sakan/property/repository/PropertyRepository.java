package com.sakan.property.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import com.sakan.property.domain.Property;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Property p where p.id = :id")
    java.util.Optional<Property> findByIdForUpdate(@Param("id") Long id);

    @Query("select p from Property p where p.ownerId = :userId or p.agentId = :userId")
    Page<Property> findManagedBy(@Param("userId") Long userId, Pageable pageable);
}
