package com.sakan.location.dto;

import com.sakan.location.domain.LocationType;

public record LocationResponse(Long id, Long parentId, LocationType type, String name) {}
