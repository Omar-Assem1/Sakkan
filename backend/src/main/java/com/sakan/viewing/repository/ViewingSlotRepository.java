package com.sakan.viewing.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sakan.viewing.entity.ViewingSlot;

public interface ViewingSlotRepository extends JpaRepository<ViewingSlot, Long> {

    /**
     * Half-open interval overlap: [start, end) overlaps an existing slot when
     * existing.start < end AND existing.end > start. Adjacent slots (10-11, 11-12) do NOT overlap.
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM ViewingSlot s
            WHERE s.propertyId = :propertyId
              AND s.startTime < :endTime
              AND s.endTime > :startTime
            """)
    boolean existsOverlappingSlot(
            @Param("propertyId") Long propertyId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    List<ViewingSlot> findByPropertyIdOrderByStartTimeAsc(Long propertyId);

    Optional<ViewingSlot> findByIdAndPropertyId(Long id, Long propertyId);

    /** Acquires a PostgreSQL row-level {@code FOR UPDATE} lock on the slot to serialize concurrent bookings (FR-18). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ViewingSlot s WHERE s.id = :id")
    Optional<ViewingSlot> findByIdForUpdate(@Param("id") Long id);
}
