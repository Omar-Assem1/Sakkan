package com.sakan.viewing.service;

import org.mapstruct.Mapper;

import com.sakan.viewing.dto.ViewingSlotResponse;
import com.sakan.viewing.domain.ViewingSlot;

@Mapper(componentModel = "spring")
public interface ViewingSlotMapper {

    ViewingSlotResponse toResponse(ViewingSlot slot);
}
