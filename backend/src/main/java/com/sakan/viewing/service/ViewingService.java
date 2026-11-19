package com.sakan.viewing.service;

import com.sakan.common.response.PageResponse;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.viewing.domain.ViewingStatus;
import com.sakan.viewing.dto.RequestViewingRequest;
import com.sakan.viewing.dto.ViewingResponse;

/** Use cases for viewing appointments (FR-17, FR-18, FR-19, FR-20, FR-21). */
public interface ViewingService {

    ViewingResponse requestViewing(RequestViewingRequest request, AuthenticatedUser requester);

    ViewingResponse confirmViewing(Long viewingId, AuthenticatedUser actor);

    ViewingResponse cancelViewing(Long viewingId, AuthenticatedUser actor);

    ViewingResponse completeViewing(Long viewingId, AuthenticatedUser actor);

    PageResponse<ViewingResponse> listMyViewings(
            AuthenticatedUser actor, ViewingStatus status, int page, int size);
}
