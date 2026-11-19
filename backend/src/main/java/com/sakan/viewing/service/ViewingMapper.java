package com.sakan.viewing.service;

import org.mapstruct.Mapper;

import com.sakan.viewing.domain.Viewing;
import com.sakan.viewing.dto.ViewingResponse;

@Mapper(componentModel = "spring")
public interface ViewingMapper {

    ViewingResponse toResponse(Viewing viewing);
}
