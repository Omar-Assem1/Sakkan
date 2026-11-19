package com.sakan.viewing.service;

import java.util.EnumSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.exception.ResourceNotFoundException;
import com.sakan.common.exception.SlotConflictException;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.common.statemachine.StatusTransitions;
import com.sakan.property.dto.PropertyResponse;
import com.sakan.property.service.PropertyService;
import com.sakan.viewing.domain.Viewing;
import com.sakan.viewing.domain.ViewingStatus;
import com.sakan.viewing.dto.RequestViewingRequest;
import com.sakan.viewing.dto.ViewingResponse;
import com.sakan.viewing.entity.ViewingSlot;
import com.sakan.viewing.repository.ViewingRepository;

@Service
@Transactional
public class ViewingServiceImpl implements ViewingService {

    private static final Logger log = LoggerFactory.getLogger(ViewingServiceImpl.class);

    private static final StatusTransitions<ViewingStatus> VIEWING_TRANSITIONS = StatusTransitions.of(Map.of(
            ViewingStatus.REQUESTED, EnumSet.of(ViewingStatus.CONFIRMED, ViewingStatus.CANCELLED),
            ViewingStatus.CONFIRMED, EnumSet.of(ViewingStatus.CANCELLED, ViewingStatus.COMPLETED)));

    private final ViewingConflictChecker conflictChecker;
    private final ViewingRepository viewingRepository;
    private final ViewingMapper mapper;
    private final PropertyService propertyService;

    @Autowired
    public ViewingServiceImpl(
            ViewingConflictChecker conflictChecker,
            ViewingRepository viewingRepository,
            ViewingMapper mapper,
            PropertyService propertyService) {
        this.conflictChecker = conflictChecker;
        this.viewingRepository = viewingRepository;
        this.mapper = mapper;
        this.propertyService = propertyService;
    }

    public ViewingServiceImpl(
            ViewingConflictChecker conflictChecker,
            ViewingRepository viewingRepository,
            ViewingMapper mapper) {
        this(conflictChecker, viewingRepository, mapper, null);
    }

    @Override
    public ViewingResponse requestViewing(RequestViewingRequest request, AuthenticatedUser requester) {
        ViewingSlot slot = conflictChecker.assertSlotBookable(request.slotId(), requester);

        Viewing viewing = new Viewing();
        viewing.setSlotId(slot.getId());
        viewing.setPropertyId(slot.getPropertyId());
        viewing.setBuyerId(requester.id());
        viewing.setStatus(ViewingStatus.REQUESTED);
        viewing.setNotes(request.notes());

        Viewing saved;
        try {
            saved = viewingRepository.saveAndFlush(viewing);
        } catch (DataIntegrityViolationException ex) {
            throw new SlotConflictException("Viewing slot " + slot.getId() + " is already booked");
        }

        log.info("Viewing {} requested on slot {} (property {}) by user {}",
                saved.getId(), slot.getId(), slot.getPropertyId(), requester.id());
        return mapper.toResponse(saved);
    }

    @Override
    public ViewingResponse confirmViewing(Long viewingId, AuthenticatedUser actor) {
        Viewing viewing = findViewingOrThrow(viewingId);
        requirePropertyManager(viewing.getPropertyId(), actor);
        return applyTransition(viewing, ViewingStatus.CONFIRMED, actor);
    }

    @Override
    public ViewingResponse cancelViewing(Long viewingId, AuthenticatedUser actor) {
        Viewing viewing = findViewingOrThrow(viewingId);
        if (!actor.id().equals(viewing.getBuyerId())) {
            requirePropertyManager(viewing.getPropertyId(), actor);
        }
        return applyTransition(viewing, ViewingStatus.CANCELLED, actor);
    }

    @Override
    public ViewingResponse completeViewing(Long viewingId, AuthenticatedUser actor) {
        Viewing viewing = findViewingOrThrow(viewingId);
        requirePropertyManager(viewing.getPropertyId(), actor);
        return applyTransition(viewing, ViewingStatus.COMPLETED, actor);
    }

    private Viewing findViewingOrThrow(Long viewingId) {
        return viewingRepository.findById(viewingId)
                .orElseThrow(() -> new ResourceNotFoundException("Viewing " + viewingId + " not found"));
    }

    private void requirePropertyManager(Long propertyId, AuthenticatedUser actor) {
        PropertyResponse property = propertyService.getById(propertyId, actor);
        boolean isManager = actor.id().equals(property.ownerId()) || actor.id().equals(property.agentId());
        if (!isManager) {
            throw new ForbiddenOperationException(
                    "Only the property owner or assigned agent can perform this action on viewing for property "
                            + propertyId);
        }
    }

    private ViewingResponse applyTransition(Viewing viewing, ViewingStatus target, AuthenticatedUser actor) {
        ViewingStatus previous = viewing.getStatus();
        VIEWING_TRANSITIONS.assertAllowed(previous, target);
        viewing.setStatus(target);
        Viewing saved = viewingRepository.save(viewing);
        log.info("Viewing {} transitioned {} -> {} by user {}", saved.getId(), previous, target, actor.id());
        return mapper.toResponse(saved);
    }
}
