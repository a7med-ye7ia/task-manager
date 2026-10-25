package com.taskmanagesys.user.service;

import com.taskmanagesys.common.error.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class AvatarService {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB

    private final Path uploadDirectory;

    public AvatarService(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.uploadDirectory = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create avatar upload directory: " + this.uploadDirectory, e);
        }
    }

    public String saveAvatar(MultipartFile file, String previousAvatarPath) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Avatar file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "The image must be 2 MB or smaller");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Could not read avatar file");
        }

        String extension = detectImageTypeFromBytes(bytes);
        if (extension == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Invalid image format. Allowed formats are JPG, PNG, and WebP");
        }

        // Verify successful image decode
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Corrupted or unreadable image file");
            }
        } catch (IOException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Corrupted or unreadable image file");
        }

        // Delete previous avatar file if exists
        deleteOldAvatar(previousAvatarPath);

        // Save with unique random name
        String filename = UUID.randomUUID() + "." + extension;
        Path targetPath = this.uploadDirectory.resolve(filename);
        try {
            Files.write(targetPath, bytes);
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_WRITE_ERROR", "Failed to save avatar file");
        }

        return filename;
    }

    public byte[] loadAvatar(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Avatar not found");
        }
        Path filePath = this.uploadDirectory.resolve(filename).normalize();
        if (!filePath.startsWith(this.uploadDirectory) || !Files.exists(filePath)) {
            throw new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Avatar not found");
        }
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_READ_ERROR", "Could not read avatar file");
        }
    }

    public String getContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    public void deleteOldAvatar(String filename) {
        if (filename != null && !filename.isBlank()) {
            try {
                Path oldPath = this.uploadDirectory.resolve(filename).normalize();
                if (oldPath.startsWith(this.uploadDirectory)) {
                    Files.deleteIfExists(oldPath);
                }
            } catch (IOException ignored) {}
        }
    }

    private String detectImageTypeFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;

        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47 &&
                bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "png";
        }
        // WebP: RIFF .... WEBP
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
                bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
