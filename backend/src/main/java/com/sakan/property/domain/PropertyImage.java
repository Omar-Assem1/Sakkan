package com.sakan.property.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "property_image")
public class PropertyImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "property_id", nullable = false)
    private Long propertyId;
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;
    @Column(name = "thumbnail_path", nullable = false, length = 500)
    private String thumbnailPath;
    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;
    @Column(name = "file_size", nullable = false)
    private long fileSize;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @Column(name = "is_primary", nullable = false)
    private boolean primary;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private boolean archived;

    @PrePersist
    void created() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long v) {
        propertyId = v;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String v) {
        storagePath = v;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String v) {
        thumbnailPath = v;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String v) {
        mimeType = v;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long v) {
        fileSize = v;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int v) {
        displayOrder = v;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean v) {
        primary = v;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean v) {
        archived = v;
    }
}
