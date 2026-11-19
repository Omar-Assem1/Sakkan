package com.sakan.viewing.service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.exception.ResourceNotFoundException;
import com.sakan.common.exception.SlotConflictException;
import com.sakan.common.response.PageResponse;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.common.statemachine.StatusTransitions;
import com.sakan.property.dto.PropertyResponse;
import com.sakan.property.service.PropertyService;
import com.sakan.viewing.domain.Viewing;
import com.sakan.viewing.domain.ViewingSlot;
import com.sakan.viewing.domain.ViewingStatus;
import com.sakan.viewing.dto.RequestViewingRequest;
import com.sakan.viewing.dto.ViewingResponse;
import com.sakan.viewing.repository.ViewingRepository;
import com.sakan.viewing.repository.ViewingSlotRepository;

@Service
@Transactional
public class ViewingServiceImpl implements ViewingService {

    private static final Logger log = LoggerFactory.getLogger(ViewingServiceImpl.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private static final StatusTransitions<ViewingStatus> VIEWING_TRANSITIONS = StatusTransitions.of(Map.of(
            ViewingStatus.REQUESTED, EnumSet.of(ViewingStatus.CONFIRMED, ViewingStatus.CANCELLED),
            ViewingStatus.CONFIRMED, EnumSet.of(ViewingStatus.CANCELLED, ViewingStatus.COMPLETED)));

    private final ViewingConflictChecker conflictChecker;
    private final ViewingRepository viewingRepository;
    private final ViewingSlotRepository slotRepository;
    private final ViewingMapper mapper;
    private final PropertyService propertyService;

    @Autowired
    public ViewingServiceImpl(
            ViewingConflictChecker conflictChecker,
            ViewingRepository viewingRepository,
            ViewingSlotRepository slotRepository,
            ViewingMapper mapper,
            PropertyService propertyService) {
        this.conflictChecker = conflictChecker;
        this.viewingRepository = viewingRepository;
        this.slotRepository = slotRepository;
        this.mapper = mapper;
        this.propertyService = propertyService;
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
        String propertyTitle = resolvePropertyTitle(slot.getPropertyId(), requester);
        return mapper.toResponse(saved, slot, propertyTitle);
    }

    @Override
    public ViewingResponse confirmViewing(Long viewingId, AuthenticatedUser actor) {
        Viewing viewing = findViewingOrThrow(viewingId);
        PropertyResponse property = requirePropertyManager(viewing.getPropertyId(), actor);
        return applyTransition(viewing, ViewingStatus.CONFIRMED, actor, property.title());
    }

    @Override
    public ViewingResponse cancelViewing(Long viewingId, AuthenticatedUser actor) {
        Viewing viewing = findViewingOrThrow(viewingId);
        String propertyTitle;
        if (actor.id().equals(viewing.getBuyerId())) {
            propertyTitle = null;
        } else {
            propertyTitle = requirePropertyManager(viewing.getPropertyId(), actor).title();
        }
        return applyTransition(viewing, ViewingStatus.CANCELLED, actor, propertyTitle);
    }

    @Override
    public ViewingResponse completeViewing(Long viewingId, AuthenticatedUser actor) {
        Viewing viewing = findViewingOrThrow(viewingId);
        PropertyResponse property = requirePropertyManager(viewing.getPropertyId(), actor);
        return applyTransition(viewing, ViewingStatus.COMPLETED, actor, property.title());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ViewingResponse> listMyViewings(
            AuthenticatedUser actor, ViewingStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Viewing> result;
        Map<Long, String> propertyTitles;
        if (actor.canManageListings()) {
            propertyTitles = loadManagedPropertyTitles(actor);
            if (propertyTitles.isEmpty()) {
                return PageResponse.from(Page.empty(pageable));
            }
            result = (status != null)
                    ? viewingRepository.findByPropertyIdInAndStatus(propertyTitles.keySet(), status, pageable)
                    : viewingRepository.findByPropertyIdIn(propertyTitles.keySet(), pageable);
        } else {
            result = (status != null)
                    ? viewingRepository.findByBuyerIdAndStatus(actor.id(), status, pageable)
                    : viewingRepository.findByBuyerId(actor.id(), pageable);
            propertyTitles = resolveDistinctPropertyTitles(result, actor);
        }

        Map<Long, ViewingSlot> slotsById = loadSlotsById(result);
        Page<ViewingResponse> mapped = result.map(v -> mapper.toResponse(
                v, slotsById.get(v.getSlotId()), propertyTitles.get(v.getPropertyId())));
        return PageResponse.from(mapped);
    }

    private Map<Long, String> loadManagedPropertyTitles(AuthenticatedUser actor) {
        Map<Long, String> titles = new LinkedHashMap<>();
        int currentPage = 0;
        PageResponse<PropertyResponse> batch;
        do {
            batch = propertyService.listMine(actor, currentPage, MAX_PAGE_SIZE);
            batch.content().forEach(p -> titles.put(p.id(), p.title()));
            currentPage++;
        } while (currentPage < batch.totalPages());
        return titles;
    }

    private Map<Long, String> resolveDistinctPropertyTitles(Page<Viewing> page, AuthenticatedUser actor) {
        Map<Long, String> titles = new HashMap<>();
        Set<Long> propertyIds = page.getContent().stream()
                .map(Viewing::getPropertyId)
                .collect(Collectors.toSet());
        for (Long propertyId : propertyIds) {
            String title = resolvePropertyTitle(propertyId, actor);
            if (title != null) {
                titles.put(propertyId, title);
            }
        }
        return titles;
    }

    private Map<Long, ViewingSlot> loadSlotsById(Page<Viewing> page) {
        Set<Long> slotIds = page.getContent().stream()
                .map(Viewing::getSlotId)
                .collect(Collectors.toSet());
        if (slotIds.isEmpty()) {
            return Map.of();
        }
        return slotRepository.findAllById(slotIds).stream()
                .collect(Collectors.toMap(ViewingSlot::getId, Function.identity()));
    }

    private String resolvePropertyTitle(Long propertyId, AuthenticatedUser viewer) {
        if (propertyService == null) {
            return null;
        }
        try {
            PropertyResponse property = propertyService.getById(propertyId, viewer);
            return property != null ? property.title() : null;
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }

    private int clampSize(int size) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Viewing findViewingOrThrow(Long viewingId) {
        return viewingRepository.findById(viewingId)
                .orElseThrow(() -> new ResourceNotFoundException("Viewing " + viewingId + " not found"));
    }

    private PropertyResponse requirePropertyManager(Long propertyId, AuthenticatedUser actor) {
        PropertyResponse property = propertyService.getById(propertyId, actor);
        boolean isManager = actor.id().equals(property.ownerId()) || actor.id().equals(property.agentId());
        if (!isManager) {
            throw new ForbiddenOperationException(
                    "Only the property owner or assigned agent can perform this action on viewing for property "
                            + propertyId);
        }
        return property;
    }

    private ViewingResponse applyTransition(
            Viewing viewing, ViewingStatus target, AuthenticatedUser actor, String propertyTitle) {
        ViewingStatus previous = viewing.getStatus();
        VIEWING_TRANSITIONS.assertAllowed(previous, target);
        viewing.setStatus(target);
        Viewing saved = viewingRepository.save(viewing);
        log.info("Viewing {} transitioned {} -> {} by user {}", saved.getId(), previous, target, actor.id());
        ViewingSlot slot = slotRepository.findById(saved.getSlotId()).orElse(null);
        String resolvedTitle = propertyTitle != null
                ? propertyTitle
                : resolvePropertyTitle(saved.getPropertyId(), actor);
        return mapper.toResponse(saved, slot, resolvedTitle);
    }
}
