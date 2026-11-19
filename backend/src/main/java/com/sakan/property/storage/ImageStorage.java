package com.sakan.property.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public interface ImageStorage {
    String save(UUID id, String variant, byte[] bytes) throws IOException;

    byte[] load(String key) throws IOException;

    void delete(String key) throws IOException;

    Path resolve(String key);
}
