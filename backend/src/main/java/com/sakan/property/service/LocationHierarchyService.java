package com.sakan.property.service;

import java.util.List;

import org.springframework.stereotype.Service;

// TEMP STUB: replace when SKN-8/9 is merged
public interface LocationHierarchyService {
    List<Long> findDescendantLocationIds(Long rootId);
}

@Service
class DefaultLocationHierarchyService implements LocationHierarchyService {
    @Override
    public List<Long> findDescendantLocationIds(Long rootId) {
        if (rootId == null) {
            return List.of();
        }
        // Temporary fallback returning the target node itself until M4 provides recursive CTE query
        return List.of(rootId);
    }
}
