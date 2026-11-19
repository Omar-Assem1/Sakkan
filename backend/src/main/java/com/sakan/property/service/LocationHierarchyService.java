package com.sakan.property.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sakan.location.repository.LocationRepository;

public interface LocationHierarchyService {
    List<Long> findDescendantLocationIds(Long rootId);
}

@Service
class DefaultLocationHierarchyService implements LocationHierarchyService {

    private final LocationRepository locations;

    DefaultLocationHierarchyService(LocationRepository locations) {
        this.locations = locations;
    }

    @Override
    public List<Long> findDescendantLocationIds(Long rootId) {
        if (rootId == null) {
            return List.of();
        }
        return locations.findDescendantIds(rootId);
    }
}
