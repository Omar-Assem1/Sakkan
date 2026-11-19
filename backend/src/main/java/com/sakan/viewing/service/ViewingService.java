package com.sakan.viewing.service;

import com.sakan.common.security.AuthenticatedUser;
import com.sakan.viewing.dto.RequestViewingRequest;
import com.sakan.viewing.dto.ViewingResponse;

/** Use cases for viewing appointments (FR-17, FR-18). */
public interface ViewingService {

    ViewingResponse requestViewing(RequestViewingRequest request, AuthenticatedUser requester);

    // TODO(SCRUM-103): implement confirm, cancel, and complete status transitions.
    ViewingResponse confirmViewing(Long viewingId, AuthenticatedUser actor);

    ViewingResponse cancelViewing(Long viewingId, AuthenticatedUser actor);

    ViewingResponse completeViewing(Long viewingId, AuthenticatedUser actor);
}
