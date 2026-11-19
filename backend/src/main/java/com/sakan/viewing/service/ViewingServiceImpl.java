package com.sakan.viewing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sakan.common.exception.SlotConflictException;
import com.sakan.common.security.AuthenticatedUser;
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

    private final ViewingConflictChecker conflictChecker;
    private final ViewingRepository viewingRepository;
    private final ViewingMapper mapper;

    public ViewingServiceImpl(
            ViewingConflictChecker conflictChecker,
            ViewingRepository viewingRepository,
            ViewingMapper mapper) {
        this.conflictChecker = conflictChecker;
        this.viewingRepository = viewingRepository;
        this.mapper = mapper;
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
}
