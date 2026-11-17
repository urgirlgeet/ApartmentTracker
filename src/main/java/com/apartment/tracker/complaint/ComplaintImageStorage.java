package com.apartment.tracker.complaint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ComplaintImageStorage {
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 6000;
    private static final long MAX_IMAGE_PIXELS = 20_000_000;

    private final Path uploadDirectory;

    public ComplaintImageStorage(@Value("${app.uploads.directory:uploads}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public StoredImage store(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return null;
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("The image must be 5 MB or smaller.");
        }

        byte[] content = image.getBytes();
        ImageType imageType = detectImageType(content);
        validateImageDimensions(content, imageType);

        Files.createDirectories(uploadDirectory);
        String filename = UUID.randomUUID() + imageType.extension();
        Path destination = resolve(filename);
        Files.copy(new ByteArrayInputStream(content), destination);
        return new StoredImage(filename, imageType.contentType());
    }

    public StoredImageFile load(String filename, String contentType) {
        Path path = resolve(filename);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("The complaint image is missing from storage.");
        }
        return new StoredImageFile(path, contentType);
    }

    public void delete(String filename) {
        try {
            Files.deleteIfExists(resolve(filename));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not remove the uncommitted complaint image.", exception);
        }
    }

    private Path resolve(String filename) {
        Path resolved = uploadDirectory.resolve(filename).normalize();
        if (!resolved.getParent().equals(uploadDirectory)) {
            throw new IllegalArgumentException("Invalid stored image filename.");
        }
        return resolved;
    }

    private ImageType detectImageType(byte[] content) {
        boolean isPng = content.length >= 8
            && content[0] == (byte) 0x89 && content[1] == 0x50 && content[2] == 0x4e
            && content[3] == 0x47 && content[4] == 0x0d && content[5] == 0x0a
            && content[6] == 0x1a && content[7] == 0x0a;
        if (isPng) {
            return ImageType.PNG;
        }
        boolean isJpeg = content.length >= 3
            && content[0] == (byte) 0xff && content[1] == (byte) 0xd8 && content[2] == (byte) 0xff;
        if (isJpeg) {
            return ImageType.JPEG;
        }
        throw new IllegalArgumentException("Choose a valid JPEG or PNG image.");
    }

    private void validateImageDimensions(byte[] content, ImageType expectedType) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw new IllegalArgumentException("The uploaded file is not a readable image.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("The uploaded file is not a readable image.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName();
                if (!expectedType.format().equalsIgnoreCase(format)) {
                    throw new IllegalArgumentException("The image content does not match its file type.");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION
                    || height > MAX_IMAGE_DIMENSION || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("Image dimensions must be at most 6000 by 6000 pixels.");
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private enum ImageType {
        JPEG(".jpg", "image/jpeg", "JPEG"),
        PNG(".png", "image/png", "PNG");

        private final String extension;
        private final String contentType;
        private final String format;

        ImageType(String extension, String contentType, String format) {
            this.extension = extension;
            this.contentType = contentType;
            this.format = format;
        }

        String extension() {
            return extension;
        }

        String contentType() {
            return contentType;
        }

        String format() {
            return format;
        }
    }

    public record StoredImage(String filename, String contentType) {}

    public record StoredImageFile(Path path, String contentType) {}
}
