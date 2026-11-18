package com.sakan.property.service;

import java.util.List;

import org.springframework.stereotype.Service;

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
        return List.of(rootId);
    }
}
