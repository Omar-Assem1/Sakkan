package com.sakan.viewing.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.exception.InvalidStatusTransitionException;
import com.sakan.common.exception.ResourceNotFoundException;
import com.sakan.common.exception.SlotConflictException;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertyResponse;
import com.sakan.property.service.PropertyService;
import com.sakan.viewing.dto.CreateViewingSlotRequest;
import com.sakan.viewing.dto.ViewingSlotResponse;
import com.sakan.viewing.domain.ViewingSlot;
import com.sakan.viewing.repository.ViewingSlotRepository;

@Service
@Transactional
public class ViewingSlotServiceImpl implements ViewingSlotService {

    private static final Logger log = LoggerFactory.getLogger(ViewingSlotServiceImpl.class);

    private final ViewingSlotRepository slotRepository;
    private final PropertyService propertyService;
    private final ViewingSlotMapper mapper;

    public ViewingSlotServiceImpl(
            ViewingSlotRepository slotRepository,
            PropertyService propertyService,
            ViewingSlotMapper mapper) {
        this.slotRepository = slotRepository;
        this.propertyService = propertyService;
        this.mapper = mapper;
    }

    @Override
    public ViewingSlotResponse createSlot(Long propertyId, CreateViewingSlotRequest request, AuthenticatedUser actor) {
        requireListingRole(actor);
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        PropertyResponse property = propertyService.getById(propertyId, actor);
        requireManager(property, actor);
        if (property.status() != PropertyStatus.PUBLISHED) {
            throw new InvalidStatusTransitionException(
                    "Viewing slots can only be added to PUBLISHED properties (property " + propertyId
                            + " is " + property.status() + ")");
        }
        if (slotRepository.existsOverlappingSlot(propertyId, request.startTime(), request.endTime())) {
            throw new SlotConflictException(
                    "Viewing slot overlaps with an existing slot on property " + propertyId);
        }

        ViewingSlot slot = new ViewingSlot();
        slot.setPropertyId(propertyId);
        slot.setCreatedBy(actor.id());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        ViewingSlot saved = slotRepository.save(slot);
        log.info("Viewing slot {} created on property {} by user {}", saved.getId(), propertyId, actor.id());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewingSlotResponse> listSlotsForProperty(Long propertyId, AuthenticatedUser viewer) {
        // Reuses M2's visibility rules: 404 unless PUBLISHED or the viewer manages the property.
        propertyService.getById(propertyId, viewer);
        return slotRepository.findByPropertyIdOrderByStartTimeAsc(propertyId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public void deleteSlot(Long propertyId, Long slotId, AuthenticatedUser actor) {
        requireListingRole(actor);
        PropertyResponse property = propertyService.getById(propertyId, actor);
        requireManager(property, actor);
        ViewingSlot slot = slotRepository.findByIdAndPropertyId(slotId, propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Viewing slot " + slotId + " not found on property " + propertyId));
        try {
            slotRepository.delete(slot);
            slotRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new SlotConflictException(
                    "Cannot delete viewing slot " + slotId + " because it has associated viewings");
        }
        log.info("Viewing slot {} deleted from property {} by user {}", slotId, propertyId, actor.id());
    }

    private void requireListingRole(AuthenticatedUser actor) {
        if (!actor.canManageListings()) {
            throw new ForbiddenOperationException("Only owners and agents can manage viewing slots");
        }
    }

    private void requireManager(PropertyResponse property, AuthenticatedUser actor) {
        boolean isManager = actor.id().equals(property.ownerId()) || actor.id().equals(property.agentId());
        if (!isManager) {
            throw new ForbiddenOperationException(
                    "Only the property owner or assigned agent can manage viewing slots");
        }
    }
}
