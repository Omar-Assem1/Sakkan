package com.sakan.property.storage;

import com.sakan.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Component
public class LocalImageStorage implements ImageStorage {
    private final Path root;

    public LocalImageStorage(@Value("${app.images.root:./uploads}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public String save(UUID id, String variant, byte[] bytes) throws IOException {
        if (!variant.equals("full") && !variant.equals("thumb"))
            throw new IllegalArgumentException("Invalid image variant");
        String key = id + "-" + variant + ".jpg";
        Path target = resolve(key);
        Files.createDirectories(root);
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        return key;
    }

    public byte[] load(String key) throws IOException {
        Path target = resolve(key);
        if (!Files.exists(target)) {
            throw new ResourceNotFoundException("Image not found");
        }
        return Files.readAllBytes(target);
    }

    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    public Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root) || !key.matches("[0-9a-fA-F-]{36}-(full|thumb)\\.jpg"))
            throw new IllegalArgumentException("Invalid image key");
        return target;
    }
}
