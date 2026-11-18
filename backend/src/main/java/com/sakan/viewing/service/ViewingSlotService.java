package com.sakan.viewing.service;

import java.util.List;

import com.sakan.common.security.AuthenticatedUser;
import com.sakan.viewing.dto.CreateViewingSlotRequest;
import com.sakan.viewing.dto.ViewingSlotResponse;

public interface ViewingSlotService {

    ViewingSlotResponse createSlot(Long propertyId, CreateViewingSlotRequest request, AuthenticatedUser actor);

    /** {@code viewer} may be null for anonymous callers; only PUBLISHED properties are visible to them. */
    List<ViewingSlotResponse> listSlotsForProperty(Long propertyId, AuthenticatedUser viewer);

    void deleteSlot(Long propertyId, Long slotId, AuthenticatedUser actor);
}
