package com.sakan.property.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sakan.property.domain.Property;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    @Query("select p from Property p where p.ownerId = :userId or p.agentId = :userId")
    Page<Property> findManagedBy(@Param("userId") Long userId, Pageable pageable);
}
