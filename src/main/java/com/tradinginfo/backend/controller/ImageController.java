package com.tradinginfo.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

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
            // URL decode the filename (frontend sends URL encoded filenames)
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
            log.info("Serving image: {}", decodedFilename);

            Path imagePath = findImagePath(decodedFilename)
                    .orElse(null);

            if (imagePath == null || !imagePath.toFile().exists()) {
                log.warn("Image not found: {}", decodedFilename);
                return ResponseEntity.notFound().build();
            }

            FileSystemResource resource = new FileSystemResource(imagePath.toFile());
            String contentType = determineContentType(decodedFilename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + decodedFilename + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error serving image: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private Optional<Path> findImagePath(String filename) {
        // Try multiple possible locations for the image
        java.util.List<String> possiblePaths = Arrays.asList(
                // Direct path in upload directory
                uploadPath + "/" + filename,
                // In Beginner lessons folder
                uploadPath + "/Beginner lessons/" + filename,
                // In any folder (search all subdirectories)
                findInSubdirectories(filename),
                // Fallback locations
                "images/" + filename,
                "lessons/images/" + filename,
                "static/images/" + filename,
                filename
        );

        return possiblePaths.stream()
                .filter(path -> path != null && !path.isEmpty())
                .map(Paths::get)
                .filter(path -> path.toFile().exists())
                .findFirst();
    }

    private String findInSubdirectories(String filename) {
        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!uploadDir.toFile().exists()) {
                return null;
            }

            // Search in all subdirectories of upload path
            return java.nio.file.Files.walk(uploadDir)
                    .filter(path -> path.getFileName().toString().equals(filename))
                    .map(Path::toString)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Error searching for image in subdirectories: {}", filename, e);
            return null;
        }
    }

    private String determineContentType(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "application/octet-stream";
        }

        String extension = filename.substring(lastDotIndex + 1).toLowerCase();
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "ico" -> "image/x-icon";
            default -> "application/octet-stream";
        };
    }
}