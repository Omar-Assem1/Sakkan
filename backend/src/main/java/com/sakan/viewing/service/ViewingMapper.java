package com.sakan.viewing.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.sakan.viewing.domain.Viewing;
import com.sakan.viewing.domain.ViewingSlot;
import com.sakan.viewing.dto.ViewingResponse;

@Mapper(componentModel = "spring")
public interface ViewingMapper {

    @Mapping(target = "id", source = "viewing.id")
    @Mapping(target = "slotId", source = "viewing.slotId")
    @Mapping(target = "propertyId", source = "viewing.propertyId")
    @Mapping(target = "buyerId", source = "viewing.buyerId")
    @Mapping(target = "status", source = "viewing.status")
    @Mapping(target = "notes", source = "viewing.notes")
    @Mapping(target = "createdAt", source = "viewing.createdAt")
    @Mapping(target = "updatedAt", source = "viewing.updatedAt")
    @Mapping(target = "slotStartTime", source = "slot.startTime")
    @Mapping(target = "slotEndTime", source = "slot.endTime")
    @Mapping(target = "propertyTitle", source = "propertyTitle")
    ViewingResponse toResponse(Viewing viewing, ViewingSlot slot, String propertyTitle);
}
