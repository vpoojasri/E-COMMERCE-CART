package com.ecommerce.service;

import com.ecommerce.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Path uploadDir;
    private final long maxFileSizeBytes;

    public FileStorageService(
            @Value("${upload.dir:./uploads/products}") String uploadDirPath,
            @Value("${upload.max_file_size:5242880}") long maxFileSizeBytes) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize file upload directory: " + this.uploadDir, e);
        }
    }

    public String storeProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a non-empty image file to upload.");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("Image size exceeds the maximum permitted limit of " + (maxFileSizeBytes / (1024 * 1024)) + " MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BadRequestException("Invalid or unsafe filename.");
        }

        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported file extension. Only JPEG, PNG, and WebP images are permitted.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Unsupported MIME type. Only image/jpeg, image/png, and image/webp are allowed.");
        }

        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(12);
            if (!isValidImageSignature(header, extension)) {
                throw new BadRequestException("File binary signature does not match a valid JPEG, PNG, or WebP image.");
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to inspect uploaded file contents.");
        }

        String safeFilename = "prod-" + UUID.randomUUID() + "." + extension;
        Path targetLocation = this.uploadDir.resolve(safeFilename).normalize();
        if (!targetLocation.startsWith(this.uploadDir)) {
            throw new BadRequestException("Security error: cannot store file outside the designated upload directory.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save uploaded image file.", e);
        }

        return "/api/uploads/" + safeFilename;
    }

    public Path resolveUploadedFile(String filename) {
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new BadRequestException("Invalid filename requested.");
        }
        Path resolved = this.uploadDir.resolve(filename).normalize();
        if (!resolved.startsWith(this.uploadDir)) {
            throw new BadRequestException("Invalid file path traversal attempt.");
        }
        return resolved;
    }

    private String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isValidImageSignature(byte[] header, String ext) {
        if (header == null || header.length < 4) {
            return false;
        }
        // JPEG: FF D8 FF
        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ("png".equals(ext)) {
            return header.length >= 8
                    && (header[0] & 0xFF) == 0x89
                    && (header[1] & 0xFF) == 0x50
                    && (header[2] & 0xFF) == 0x4E
                    && (header[3] & 0xFF) == 0x47;
        }
        // WebP: "RIFF" .... "WEBP"
        if ("webp".equals(ext)) {
            return header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
        }
        return false;
    }
}

