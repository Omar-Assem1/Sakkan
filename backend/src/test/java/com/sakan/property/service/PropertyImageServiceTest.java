package com.sakan.property.service;

import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.property.domain.Property;
import com.sakan.property.domain.PropertyImage;
import com.sakan.property.repository.PropertyImageRepository;
import com.sakan.property.repository.PropertyRepository;
import com.sakan.property.storage.ImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyImageServiceTest {
    @Mock
    PropertyRepository properties;
    @Mock
    PropertyImageRepository images;
    @Mock
    ImageStorage storage;
    PropertyImageService service;
    Property property;

    @BeforeEach
    void setup() {
        service = new PropertyImageService(properties, images, storage);
        property = new Property();
        property.setOwnerId(7L);
        when(properties.findByIdForUpdate(12L)).thenReturn(Optional.of(property));
    }

    @Test
    void uploadsMultipleDecodedImagesAndPersistsThumbnailPaths() throws Exception {
        when(images.countByPropertyIdAndArchivedFalse(12L)).thenReturn(0L);
        when(images.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storage.save(any(), anyString(), any()))
                .thenAnswer(inv -> inv.getArgument(0).toString() + "-" + inv.getArgument(1) + ".jpg");
        var result = service.upload(12L, List.of(png("one.png"), png("two.png")),
                new AuthenticatedUser(7L, Set.of("OWNER")));
        assertEquals(2, result.size());
        assertTrue(result.get(0).thumbnailUrl().endsWith("-thumb.jpg"));
        verify(storage, times(4)).save(any(), anyString(), any());
        verify(images).flush();
    }

    @Test
    void rejectsMoreThanTenActiveImages() {
        when(images.countByPropertyIdAndArchivedFalse(12L)).thenReturn(9L);
        assertThrows(IllegalArgumentException.class, () -> service.upload(12L, List.of(png("1.png"), png("2.png")),
                new AuthenticatedUser(7L, Set.of("OWNER"))));
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsNonManager() {
        assertThrows(ForbiddenOperationException.class,
                () -> service.upload(12L, List.of(png("1.png")), new AuthenticatedUser(8L, Set.of("OWNER"))));
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsCorruptImage() {
        when(images.countByPropertyIdAndArchivedFalse(12L)).thenReturn(0L);
        assertThrows(IllegalArgumentException.class,
                () -> service.upload(12L,
                        List.of(new MockMultipartFile("files", "bad.png", "image/png", new byte[] { 1, 2, 3 })),
                        new AuthenticatedUser(7L, Set.of("OWNER"))));
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsOversizedImage() {
        when(images.countByPropertyIdAndArchivedFalse(12L)).thenReturn(0L);
        var oversized = new MockMultipartFile("files", "large.jpg", "image/jpeg",
                new byte[(int) PropertyImageService.MAX_BYTES + 1]);
        assertThrows(IllegalArgumentException.class,
                () -> service.upload(12L, List.of(oversized), new AuthenticatedUser(7L, Set.of("OWNER"))));
        verifyNoInteractions(storage);
    }

    private MockMultipartFile png(String name) throws Exception {
        BufferedImage image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile("files", name, "image/png", out.toByteArray());
    }
}
