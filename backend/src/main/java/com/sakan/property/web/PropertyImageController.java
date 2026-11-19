package com.sakan.property.web;

import com.sakan.common.security.CurrentUserProvider;
import com.sakan.property.dto.PropertyImageResponse;
import com.sakan.property.service.PropertyImageService;
import com.sakan.property.storage.ImageStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Property images")
public class PropertyImageController {
    private final PropertyImageService service;
    private final CurrentUserProvider users;
    private final ImageStorage storage;

    public PropertyImageController(PropertyImageService s, CurrentUserProvider u, ImageStorage storage) {
        service = s;
        users = u;
        this.storage = storage;
    }

    @Operation(summary = "List active property images")
    @GetMapping("/properties/{id}/images")
    public List<PropertyImageResponse> list(@PathVariable long id) {
        return service.list(id);
    }

    @Operation(summary = "Upload property images (maximum 10 active)")
    @PostMapping(value = "/properties/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<PropertyImageResponse> upload(@PathVariable long id, @RequestParam("files") List<MultipartFile> files) {
        return service.upload(id, files, users.require());
    }

    @Operation(summary = "Reorder active property images")
    @PutMapping("/properties/{id}/images/order")
    public void reorder(@PathVariable long id, @RequestBody List<Long> imageIds) {
        service.reorder(id, imageIds, users.require());
    }

    @Operation(summary = "Archive a property image")
    @DeleteMapping("/properties/{id}/images/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable long id, @PathVariable long imageId) {
        service.delete(id, imageId, users.require());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/property-images/{key}")
    public ResponseEntity<byte[]> image(@PathVariable String key) throws IOException {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic()).body(storage.load(key));
    }
}
