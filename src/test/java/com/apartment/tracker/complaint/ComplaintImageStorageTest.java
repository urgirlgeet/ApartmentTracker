package com.apartment.tracker.complaint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ComplaintImageStorageTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void storesValidPngUsingGeneratedFilename() throws IOException {
        ComplaintImageStorage storage = new ComplaintImageStorage(temporaryDirectory.toString());
        MockMultipartFile image = new MockMultipartFile(
            "image", "resident-upload.png", "image/png", pngImage());

        ComplaintImageStorage.StoredImage stored = storage.store(image);
        ComplaintImageStorage.StoredImageFile loaded = storage.load(stored.filename(), stored.contentType());

        assertEquals("image/png", loaded.contentType());
        assertTrue(stored.filename().matches("[0-9a-f-]{36}\\.png"));
        assertTrue(Files.isRegularFile(loaded.path()));
    }

    @Test
    void rejectsNonImageContentEvenWhenNamedAsPng() {
        ComplaintImageStorage storage = new ComplaintImageStorage(temporaryDirectory.toString());
        MockMultipartFile fakeImage = new MockMultipartFile(
            "image", "not-an-image.png", "image/png", "not an image".getBytes());

        assertThrows(IllegalArgumentException.class, () -> storage.store(fakeImage));
    }

    @Test
    void rejectsImagesOverFiveMegabytes() {
        ComplaintImageStorage storage = new ComplaintImageStorage(temporaryDirectory.toString());
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile image = new MockMultipartFile("image", "large.png", "image/png", oversized);

        assertThrows(IllegalArgumentException.class, () -> storage.store(image));
    }

    private byte[] pngImage() throws IOException {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
