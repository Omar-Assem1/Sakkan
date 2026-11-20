package com.sakan.property.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.sakan.property.domain.Property;

@Mapper(componentModel = "spring")
public interface PropertyMapper {

    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "agentId", ignore = true)
    @Mapping(target = "status", ignore = true)
    Property toEntity(PropertyRequest request);

    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "agentId", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(PropertyRequest request, @MappingTarget Property property);

    @Mapping(target = "thumbnailUrl", ignore = true)
    PropertyResponse toResponse(Property property);

    @Mapping(target = "thumbnailUrl", ignore = true)
    PropertySummaryResponse toSummaryResponse(Property property);
}
