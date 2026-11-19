package com.sakan.property.dto;

import java.time.Instant;

public record PropertyImageResponse(Long id, String url, String thumbnailUrl, String mimeType,
     long fileSize,int displayOrder, boolean primary, Instant createdAt) {
}
