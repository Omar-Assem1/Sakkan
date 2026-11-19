package com.sakan.viewing.service;

import org.springframework.stereotype.Service;

import com.sakan.common.security.AuthenticatedUser;
import com.sakan.property.service.PropertyService;
import com.sakan.viewing.entity.ViewingSlot;
import com.sakan.viewing.repository.ViewingRepository;
import com.sakan.viewing.repository.ViewingSlotRepository;

// TODO(SCRUM-99): implement ViewingConflictChecker with pessimistic row lock, self-booking check, and active viewing check.
@Service
public class ViewingConflictChecker {

    @SuppressWarnings("unused")
    private final ViewingSlotRepository slotRepository;
    @SuppressWarnings("unused")
    private final ViewingRepository viewingRepository;
    @SuppressWarnings("unused")
    private final PropertyService propertyService;

    public ViewingConflictChecker(
            ViewingSlotRepository slotRepository,
            ViewingRepository viewingRepository,
            PropertyService propertyService) {
        this.slotRepository = slotRepository;
        this.viewingRepository = viewingRepository;
        this.propertyService = propertyService;
    }

    public ViewingSlot assertSlotBookable(Long slotId, AuthenticatedUser requester) {
        throw new UnsupportedOperationException("TODO(SCRUM-99): implement ViewingConflictChecker");
    }
}
