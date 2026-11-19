package com.sakan.property.service;

import com.sakan.common.exception.*;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.property.domain.*;
import com.sakan.property.repository.*;
import com.sakan.property.storage.ImageStorage;
import com.sakan.property.dto.PropertyImageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

@Service
public class PropertyImageService {
    public static final int MAX_IMAGES = 10;
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    private final PropertyRepository properties;
    private final PropertyImageRepository images;
    private final ImageStorage storage;

    public PropertyImageService(PropertyRepository properties, PropertyImageRepository images, ImageStorage storage) {
        this.properties = properties;
        this.images = images;
        this.storage = storage;
    }

    @Transactional
    public List<PropertyImageResponse> upload(long propertyId, List<MultipartFile> files, AuthenticatedUser actor) {
        Property property = properties.findByIdForUpdate(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property " + propertyId + " not found"));
        requireManager(property, actor);
        if (files == null || files.isEmpty())
            throw new IllegalArgumentException("Select at least one image");
        if (images.countByPropertyIdAndArchivedFalse(propertyId) + files.size() > MAX_IMAGES)
            throw new IllegalArgumentException("A property can have at most 10 active images");
        List<String> written = new ArrayList<>();
        try {
            int order = (int) images.countByPropertyIdAndArchivedFalse(propertyId);
            List<PropertyImage> saved = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file.isEmpty() || file.getSize() > MAX_BYTES)
                    throw new IllegalArgumentException("Each image must be non-empty and no larger than 10 MB");
                BufferedImage decoded;
                String format;
                try (ImageInputStream in = ImageIO.createImageInputStream(file.getInputStream())) {
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
                    if (!readers.hasNext())
                        throw new IllegalArgumentException("Unsupported or corrupt image");
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(in, true, true);
                        format = reader.getFormatName().toLowerCase(Locale.ROOT);
                        if (!Set.of("jpeg", "jpg", "png", "webp").contains(format))
                            throw new IllegalArgumentException("Only JPEG, PNG and WebP images are allowed");
                        decoded = reader.read(0);
                    } finally {
                        reader.dispose();
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException("Unsupported or corrupt image", e);
                }
                if (decoded == null || decoded.getWidth() < 1 || decoded.getHeight() < 1
                        || (long) decoded.getWidth() * decoded.getHeight() > 40_000_000L)
                    throw new IllegalArgumentException("Invalid image dimensions");
                BufferedImage full = resize(decoded, 2400);
                BufferedImage thumb = resize(decoded, 480);
                byte[] fullBytes = jpeg(full), thumbBytes = jpeg(thumb);
                UUID id = UUID.randomUUID();
                String fullKey = storage.save(id, "full", fullBytes);
                written.add(fullKey);
                String thumbKey = storage.save(id, "thumb", thumbBytes);
                written.add(thumbKey);
                PropertyImage image = new PropertyImage();
                image.setPropertyId(propertyId);
                image.setStoragePath(fullKey);
                image.setThumbnailPath(thumbKey);
                image.setMimeType("image/jpeg");
                image.setFileSize(fullBytes.length);
                image.setDisplayOrder(order++);
                image.setPrimary(saved.isEmpty() && images.countByPropertyIdAndArchivedFalse(propertyId) == 0);
                saved.add(images.save(image));
            }
            images.flush();
            return saved.stream().map(this::response).toList();
        } catch (Exception ex) {
            written.forEach(key -> {
                try {
                    storage.delete(key);
                } catch (IOException ignored) {
                }
            });
            throw ex instanceof RuntimeException r ? r
                    : new IllegalStateException("Could not store property images", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<PropertyImageResponse> list(long propertyId) {
        if (!properties.existsById(propertyId))
            throw new ResourceNotFoundException("Property " + propertyId + " not found");
        return images.findByPropertyIdAndArchivedFalseOrderByDisplayOrderAsc(propertyId).stream().map(this::response)
                .toList();
    }

    @Transactional
    public void delete(long propertyId, long imageId, AuthenticatedUser actor) {
        Property p = properties.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property " + propertyId + " not found"));
        requireManager(p, actor);
        PropertyImage i = images.findByIdAndPropertyIdAndArchivedFalse(imageId, propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        i.setArchived(true);
        images.save(i);
        List<PropertyImage> active = images.findByPropertyIdAndArchivedFalseOrderByDisplayOrderAsc(propertyId);
        for (int n = 0; n < active.size(); n++) {
            active.get(n).setPrimary(n == 0);
            active.get(n).setDisplayOrder(n);
        }
        images.saveAll(active);
    }

    @Transactional
    public void reorder(long propertyId, List<Long> ids, AuthenticatedUser actor) {
        Property p = properties.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property " + propertyId + " not found"));
        requireManager(p, actor);
        List<PropertyImage> active = images.findByPropertyIdAndArchivedFalseOrderByDisplayOrderAsc(propertyId);
        if (ids.size() != active.size() || !new HashSet<>(ids)
                .equals(active.stream().map(PropertyImage::getId).collect(java.util.stream.Collectors.toSet())))
            throw new IllegalArgumentException("Order must include every active image exactly once");
        for (int n = 0; n < ids.size(); n++) {
            Long imageId = ids.get(n);
            PropertyImage i = active.stream().filter(x -> x.getId().equals(imageId)).findFirst().orElseThrow();
            i.setDisplayOrder(n);
            i.setPrimary(n == 0);
        }
        images.saveAll(active);
    }

    private void requireManager(Property p, AuthenticatedUser u) {
        if (u == null || (!u.id().equals(p.getOwnerId()) && !u.id().equals(p.getAgentId())))
            throw new ForbiddenOperationException("Only the owner or assigned agent can manage property images");
    }

    private PropertyImageResponse response(PropertyImage i) {
        return new PropertyImageResponse(i.getId(), "/api/property-images/" + i.getStoragePath(),
                "/api/property-images/" + i.getThumbnailPath(), i.getMimeType(), i.getFileSize(), i.getDisplayOrder(),
                i.isPrimary(), i.getCreatedAt());
    }

    private BufferedImage resize(BufferedImage source, int max) {
        double scale = Math.min(1d, (double) max / Math.max(source.getWidth(), source.getHeight()));
        int w = Math.max(1, (int) (source.getWidth() * scale)), h = Math.max(1, (int) (source.getHeight() * scale));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private byte[] jpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "jpeg", out))
            throw new IOException("JPEG encoder unavailable");
        return out.toByteArray();
    }
}
