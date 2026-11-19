package com.sakan.viewing.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sakan.viewing.domain.Viewing;
import com.sakan.viewing.domain.ViewingStatus;

/** Repository for {@link Viewing} appointments. */
public interface ViewingRepository extends JpaRepository<Viewing, Long> {

    boolean existsBySlotIdAndStatusIn(Long slotId, Collection<ViewingStatus> statuses);

    long countBySlotIdAndStatusIn(Long slotId, Collection<ViewingStatus> statuses);
}
