package com.sakan.viewing.service;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.exception.ResourceNotFoundException;
import com.sakan.common.exception.SlotConflictException;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.property.dto.PropertyResponse;
import com.sakan.property.service.PropertyService;
import com.sakan.viewing.domain.ViewingStatus;
import com.sakan.viewing.entity.ViewingSlot;
import com.sakan.viewing.repository.ViewingRepository;
import com.sakan.viewing.repository.ViewingSlotRepository;

/**
 * Validates that a viewing slot exists, is not owned/managed by the requester, and has no
 * active ({@code REQUESTED} or {@code CONFIRMED}) viewing while holding a pessimistic write lock
 * on the {@link ViewingSlot} row (FR-17, FR-18).
 */
@Service
public class ViewingConflictChecker {

    public static final Set<ViewingStatus> ACTIVE_STATUSES =
            EnumSet.of(ViewingStatus.REQUESTED, ViewingStatus.CONFIRMED);

    private final ViewingSlotRepository slotRepository;
    private final ViewingRepository viewingRepository;
    private final PropertyService propertyService;

    public ViewingConflictChecker(
            ViewingSlotRepository slotRepository,
            ViewingRepository viewingRepository,
            PropertyService propertyService) {
        this.slotRepository = slotRepository;
        this.viewingRepository = viewingRepository;
        this.propertyService = propertyService;
    }

    /**
     * Locks the slot row inside the caller's transaction and verifies it can be booked by {@code requester}.
     *
     * @throws ResourceNotFoundException   if the slot does not exist (404)
     * @throws ForbiddenOperationException if {@code requester} is the property's owner or assigned agent (403)
     * @throws SlotConflictException       if the slot already has an active viewing (409)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public ViewingSlot assertSlotBookable(Long slotId, AuthenticatedUser requester) {
        ViewingSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Viewing slot " + slotId + " not found"));

        PropertyResponse property = propertyService.getById(slot.getPropertyId(), requester);
        if (requester.id().equals(property.ownerId()) || requester.id().equals(property.agentId())) {
            throw new ForbiddenOperationException(
                    "Property owners and assigned agents cannot book viewings on their own property");
        }

        if (viewingRepository.existsBySlotIdAndStatusIn(slotId, ACTIVE_STATUSES)) {
            throw new SlotConflictException("Viewing slot " + slotId + " is already booked");
        }

        return slot;
    }
}
