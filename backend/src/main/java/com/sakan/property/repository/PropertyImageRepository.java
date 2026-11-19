package com.sakan.property.repository;

import com.sakan.property.domain.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {
    List<PropertyImage> findByPropertyIdAndArchivedFalseOrderByDisplayOrderAsc(Long propertyId);

    long countByPropertyIdAndArchivedFalse(Long propertyId);

    Optional<PropertyImage> findByIdAndPropertyIdAndArchivedFalse(Long id, Long propertyId);
}
