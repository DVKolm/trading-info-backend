package com.tradinginfo.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class ImageController {

    @Value("${upload.path:uploads/}")
    private String uploadPath;

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            // Decode the filename
            String decodedFilename = java.net.URLDecoder.decode(filename, "UTF-8");

            log.info("📷 Serving image: {}", decodedFilename);

            // Look for image in various possible locations
            Path imagePath = findImagePath(decodedFilename);

            if (imagePath == null || !imagePath.toFile().exists()) {
                log.warn("❌ Image not found: {}", decodedFilename);
                return ResponseEntity.notFound().build();
            }

            FileSystemResource resource = new FileSystemResource(imagePath.toFile());

            // Determine content type based on file extension
            String contentType = getContentType(decodedFilename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + decodedFilename + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("❌ Error serving image: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private Path findImagePath(String filename) {
        // Try different possible locations for images
        String[] possiblePaths = {
                uploadPath + "/" + filename,
                "images/" + filename,
                "lessons/images/" + filename,
                "static/images/" + filename,
                filename // Direct path
        };

        for (String pathStr : possiblePaths) {
            Path path = Paths.get(pathStr);
            if (path.toFile().exists()) {
                return path;
            }
        }

        return null;
    }

    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}