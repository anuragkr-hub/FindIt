package com.findit.service;

import com.findit.config.AppProperties;
import com.findit.exception.FileStorageException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private final AppProperties appProperties;

    public FileStorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.fileStorageLocation = Paths.get(appProperties.getUploadDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create directory for uploaded files at: " + this.fileStorageLocation, ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Validate file size
        if (file.getSize() > appProperties.getMaxFileSizeBytes()) {
            throw new FileStorageException("File size exceeds maximum allowed limit of " + (appProperties.getMaxFileSizeBytes() / (1024 * 1024)) + "MB");
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || !appProperties.getAllowedMimeTypes().contains(contentType.toLowerCase())) {
            throw new FileStorageException("Unsupported file type. Only JPEG, PNG, WEBP, and GIF images are allowed.");
        }

        // Get safe file extension
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg");
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        } else {
            extension = ".jpg";
        }

        // Generate clean random filename to prevent collisions and malicious paths
        String safeFileName = UUID.randomUUID().toString() + extension;

        try {
            // Check for path traversal attempt
            if (safeFileName.contains("..")) {
                throw new FileStorageException("Invalid filename sequence: " + safeFileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(safeFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + safeFileName;
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store file: " + safeFileName, ex);
        }
    }

    public Resource loadFileAsResource(String filename) {
        try {
            // Sanitize filename to prevent directory traversal
            String cleanName = Paths.get(filename).getFileName().toString();
            Path filePath = this.fileStorageLocation.resolve(cleanName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("File not found or unreadable: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("File not found: " + filename, ex);
        }
    }

    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }
}
