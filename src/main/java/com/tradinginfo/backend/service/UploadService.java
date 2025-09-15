package com.tradinginfo.backend.service;

import com.tradinginfo.backend.entity.Lesson;
import com.tradinginfo.backend.repository.LessonRepository;
import com.tradinginfo.backend.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UploadService {

    private final LessonRepository lessonRepository;
    private final Optional<TelegramBotService> telegramBotService;

    @Value("${upload.path}")
    private String uploadPath;

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    public Map<String, Object> uploadLessons(MultipartFile file, String targetFolder, Long telegramId) {
        Map<String, Object> result = new HashMap<String, Object>();
        List<String> uploadedFiles = new ArrayList<String>();
        List<String> errors = new ArrayList<String>();

        try {
            Path tempDir = Files.createTempDirectory("upload_");
            log.info("Created temp directory: {}", tempDir);
            extractZipFile(file, tempDir);
            log.info("Extracted ZIP file to temp directory");

            // Log all extracted files
            Files.walk(tempDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> log.info("Found extracted file: {}", path.getFileName()));

            // Process .md files and copy images
            List<Path> mdFiles = Files.walk(tempDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md"))
                    .collect(Collectors.toList());

            List<Path> imageFiles = Files.walk(tempDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".gif") || fileName.endsWith(".webp");
                    })
                    .collect(Collectors.toList());

            log.info("Found {} .md files and {} image files to process", mdFiles.size(), imageFiles.size());

            mdFiles.forEach(mdFile -> processMarkdownFile(mdFile, targetFolder, uploadedFiles, errors));
            imageFiles.forEach(imageFile -> copyImageFile(imageFile, tempDir, targetFolder, errors));

            deleteDirectory(tempDir);

            result.put("success", true);
            result.put("filesUploaded", uploadedFiles.size());
            result.put("files", uploadedFiles);
            if (!errors.isEmpty()) {
                result.put("errors", errors);
            }

            // sendTelegramNotificationForUpload(targetFolder, uploadedFiles);

        } catch (IOException e) {
            log.error("Upload failed due to IO error", e);
            result.put("success", false);
            result.put("error", "File processing error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Upload failed unexpectedly", e);
            result.put("success", false);
            result.put("error", "Unexpected error occurred");
        }

        return result;
    }

    private void saveLessonToDatabase(String lessonPath, String content, String targetFolder) {
        Map<String, Object> frontmatter = extractFrontmatter(content);
        String markdownContent = removeFrontmatter(content);

        String title = extractTitle(frontmatter, lessonPath);
        String htmlContent = parseMarkdownToHtml(markdownContent);
        int wordCount = countWords(markdownContent);
        Integer lessonNumber = extractLessonNumber(title);
        String fileHash = calculateHash(content);

        Lesson lesson = lessonRepository.findByPath(lessonPath).orElse(new Lesson());
        lesson.setPath(lessonPath);
        lesson.setTitle(title);
        lesson.setContent(markdownContent);
        lesson.setHtmlContent(htmlContent);
        lesson.setFrontmatter(frontmatter);
        lesson.setWordCount(wordCount);
        lesson.setParentFolder(targetFolder);
        lesson.setLessonNumber(lessonNumber);
        lesson.setFileHash(fileHash);
        lesson.setUpdatedAt(LocalDateTime.now());

        lessonRepository.save(lesson);
    }

    private Map<String, Object> extractFrontmatter(String content) {
        Map<String, Object> frontmatter = new HashMap<>();
        if (content.startsWith("---")) {
            int endIndex = content.indexOf("---", 3);
            if (endIndex > 0) {
                String yaml = content.substring(3, endIndex);
                for (String line : yaml.split("\n")) {
                    if (line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            frontmatter.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                }
            }
        }
        return frontmatter;
    }

    private String removeFrontmatter(String content) {
        if (content.startsWith("---")) {
            int endIndex = content.indexOf("---", 3);
            if (endIndex > 0) {
                return content.substring(endIndex + 3).trim();
            }
        }
        return content;
    }

    private String extractTitle(Map<String, Object> frontmatter, String path) {
        if (frontmatter.containsKey("title")) {
            return frontmatter.get("title").toString();
        }
        String fileName = Paths.get(path).getFileName().toString();
        return fileName.replace(".md", "");
    }

    private String parseMarkdownToHtml(String markdown) {
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    private int countWords(String content) {
        return content.split("\\s+").length;
    }

    private Integer extractLessonNumber(String title) {
        Pattern pattern = Pattern.compile("Урок\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(title);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return "";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private void extractZipFile(MultipartFile file, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = destDir.resolve(entry.getName());
                if (!entry.isDirectory()) {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public Map<String, Object> uploadSingleLesson(MultipartFile file, String targetFolder, Long telegramId) {
        Map<String, Object> result = new HashMap<String, Object>();

        try {
            String originalFilename = Optional.ofNullable(file.getOriginalFilename())
                    .orElseThrow(() -> new IllegalArgumentException("File name is required"));

            // Handle ZIP files by redirecting to uploadLessons method
            if (originalFilename.endsWith(".zip")) {
                log.info("ZIP file detected in single upload, redirecting to bulk upload: {}", originalFilename);
                return uploadLessons(file, targetFolder, telegramId);
            }

            if (!originalFilename.endsWith(".md")) {
                result.put("success", false);
                result.put("error", "Only Markdown (.md) files are allowed");
                return result;
            }

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String lessonPath = Optional.ofNullable(targetFolder)
                    .map(folder -> folder + "/" + originalFilename)
                    .orElse(originalFilename);

            saveLessonToDatabase(lessonPath, content, targetFolder);

            result.put("success", true);
            result.put("message", "Lesson uploaded successfully: " + originalFilename);
            result.put("fileName", originalFilename);

            log.info("Uploaded single lesson: {}", originalFilename);
            // sendTelegramNotificationForSingleLessonUpload(originalFilename, targetFolder);

        } catch (IOException e) {
            log.error("Failed to upload single lesson due to IO error", e);
            result.put("success", false);
            result.put("error", "File processing error");
        } catch (IllegalArgumentException e) {
            log.error("Invalid file upload request", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during single lesson upload", e);
            result.put("success", false);
            result.put("error", "Unexpected error occurred");
        }

        return result;
    }

    public void deleteLessonsFolder(String folder, Long telegramId) {
        List<Lesson> lessons = lessonRepository.findByParentFolder(folder);

        deletePhysicalFolderSafely(folder);
        lessonRepository.deleteAll(lessons);
        log.info("Deleted {} lessons from folder: {}", lessons.size(), folder);

        // sendTelegramNotificationForFolderDeletion(folder, lessons.size());
    }

    public void deleteSingleLesson(String lessonPath, Long telegramId) {
        Lesson lesson = lessonRepository.findByPath(lessonPath)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonPath));

        deletePhysicalFileSafely(lessonPath);
        lessonRepository.delete(lesson);
        log.info("Deleted single lesson: {}", lessonPath);

        // sendTelegramNotificationForLessonDeletion(lessonPath, lesson.getTitle());
    }

    private void processMarkdownFile(Path mdFile, String targetFolder,
                                     List<String> uploadedFiles, List<String> errors) {
        try {
            String content = Files.readString(mdFile, StandardCharsets.UTF_8);
            String fileName = mdFile.getFileName().toString();
            String lessonPath = targetFolder + "/" + fileName;

            saveLessonToDatabase(lessonPath, content, targetFolder);
            uploadedFiles.add(fileName);
            log.info("Uploaded lesson: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to process file: {}", mdFile.getFileName(), e);
            errors.add(mdFile.getFileName().toString() + ": " + e.getMessage());
        }
    }

    private void copyImageFile(Path imageFile, Path tempDir, String targetFolder, List<String> errors) {
        try {
            String fileName = imageFile.getFileName().toString();

            // Create target directory structure
            Path uploadFolderPath = Paths.get(uploadPath, targetFolder);
            if (!Files.exists(uploadFolderPath)) {
                Files.createDirectories(uploadFolderPath);
            }

            // Copy image to upload directory
            Path targetImagePath = uploadFolderPath.resolve(fileName);
            Files.copy(imageFile, targetImagePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Copied image: {} to {}", fileName, targetImagePath);
        } catch (Exception e) {
            log.error("Failed to copy image: {}", imageFile.getFileName(), e);
            errors.add("Image " + imageFile.getFileName().toString() + ": " + e.getMessage());
        }
    }

    private void sendTelegramNotificationForUpload(String targetFolder, List<String> uploadedFiles) {
        // Notification to channel disabled
        log.info("Lessons uploaded to folder: {} (files: {})", targetFolder, uploadedFiles.size());
    }

    private void sendTelegramNotificationForFolderDeletion(String folder, int lessonsCount) {
        // Notification to channel disabled
        log.info("Folder deleted: {} ({} lessons)", folder, lessonsCount);
    }

    private void sendTelegramNotificationForLessonDeletion(String lessonPath, String lessonTitle) {
        // Notification to channel disabled
        log.info("Lesson deleted: {} ({})", lessonPath, lessonTitle);
    }

    private void sendTelegramNotificationForSingleLessonUpload(String fileName, String targetFolder) {
        // Notification to channel disabled
        log.info("Single lesson uploaded: {} to folder: {}", fileName, targetFolder);
    }

    private void deletePhysicalFolderSafely(String folderPath) {
        try {
            deletePhysicalFolder(folderPath);
        } catch (IOException e) {
            log.warn("Failed to delete physical folder: {}", folderPath, e);
        }
    }

    private void deletePhysicalFileSafely(String lessonPath) {
        try {
            deletePhysicalFile(lessonPath);
        } catch (IOException e) {
            log.warn("Failed to delete physical file: {}", lessonPath, e);
        }
    }

    private void deletePhysicalFile(String lessonPath) throws IOException {
        Path filePath = Paths.get(uploadPath, lessonPath);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("🗑️ Deleted physical file: {}", filePath);
        } else {
            log.warn("⚠️ Physical file not found: {}", filePath);
        }

        // Also try to delete the file from the project root (in case it's stored there)
        Path rootFilePath = Paths.get(lessonPath);
        if (Files.exists(rootFilePath)) {
            Files.delete(rootFilePath);
            log.info("🗑️ Deleted physical file from root: {}", rootFilePath);
        }
    }

    private void deletePhysicalFolder(String folderPath) throws IOException {
        Path uploadFolderPath = Paths.get(uploadPath, folderPath);
        if (Files.exists(uploadFolderPath)) {
            deleteDirectory(uploadFolderPath);
            log.info("🗑️ Deleted physical folder: {}", uploadFolderPath);
        }

        // Also try to delete the folder from the project root
        Path rootFolderPath = Paths.get(folderPath);
        if (Files.exists(rootFolderPath)) {
            deleteDirectory(rootFolderPath);
            log.info("🗑️ Deleted physical folder from root: {}", rootFolderPath);
        }
    }


    public void createFolder(String folderName, Long telegramId) {
        createFolder(folderName, telegramId, false);
    }

    public void createFolder(String folderName, Long telegramId, Boolean subscriptionRequired) {
        if (folderName == null || folderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be empty");
        }

        String cleanFolderName = folderName.trim();

        // Check if folder already exists as a Lesson entity
        Optional<Lesson> existingFolder = lessonRepository.findByPath(cleanFolderName);
        if (existingFolder.isPresent()) {
            throw new IllegalArgumentException("Folder already exists: " + cleanFolderName);
        }

        // Create physical folder in uploads directory
        try {
            Path folderPath = Paths.get(uploadPath, cleanFolderName);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
                log.info("📁 Physical folder created: {}", folderPath);
            }
        } catch (IOException e) {
            log.error("Failed to create physical folder: {}", cleanFolderName, e);
            throw new RuntimeException("Failed to create physical folder: " + e.getMessage());
        }

        // Create folder as a Lesson entity
        Lesson folderLesson = new Lesson();
        folderLesson.setPath(cleanFolderName);
        folderLesson.setTitle(cleanFolderName);
        folderLesson.setContent("");
        folderLesson.setHtmlContent("");
        folderLesson.setIsFolder(true);
        folderLesson.setSubscriptionRequired(subscriptionRequired != null ? subscriptionRequired : false);
        folderLesson.setParentFolder(null);
        folderLesson.setWordCount(0);

        lessonRepository.save(folderLesson);

        log.info("✅ Folder created: {} with subscription required: {}", cleanFolderName, subscriptionRequired);
    }

    public void updateFolderSubscription(String folderPath, Boolean subscriptionRequired) {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder path cannot be empty");
        }

        Optional<Lesson> folder = lessonRepository.findByPath(folderPath);
        if (folder.isEmpty() || !Boolean.TRUE.equals(folder.get().getIsFolder())) {
            throw new IllegalArgumentException("Folder not found: " + folderPath);
        }

        Lesson folderLesson = folder.get();
        folderLesson.setSubscriptionRequired(subscriptionRequired != null ? subscriptionRequired : false);
        lessonRepository.save(folderLesson);

        log.info("✅ Updated folder subscription: {} to {}", folderPath, subscriptionRequired);
    }

    public Map<String, Object> getFileTreeForAdmin() {
        try {
            List<Lesson> allLessons = lessonRepository.findAll();
            List<Map<String, Object>> fileTree = new ArrayList<>();

            // Group lessons by folder
            Map<String, List<Lesson>> lessonsByFolder = allLessons.stream()
                    .filter(lesson -> lesson.getParentFolder() != null)
                    .collect(java.util.stream.Collectors.groupingBy(Lesson::getParentFolder));

            // Create folder structure for admin panel
            for (Map.Entry<String, List<Lesson>> folderEntry : lessonsByFolder.entrySet()) {
                String folderName = folderEntry.getKey();
                List<Lesson> lessons = folderEntry.getValue();

                Map<String, Object> folder = new HashMap<>();
                folder.put("id", "folder_" + Math.abs(folderName.hashCode()));
                folder.put("name", folderName);
                folder.put("type", "folder");
                folder.put("path", folderName);

                List<Map<String, Object>> children = new ArrayList<>();
                for (Lesson lesson : lessons) {
                    Map<String, Object> file = new HashMap<>();
                    // Only use basic data types, avoid Entity serialization issues
                    file.put("id", "file_" + lesson.getId());
                    file.put("name", lesson.getTitle() != null ? lesson.getTitle() : "Unnamed");
                    file.put("type", "file");
                    file.put("path", lesson.getPath() != null ? lesson.getPath() : "");
                    children.add(file);
                }
                folder.put("children", children);
                fileTree.add(folder);
            }

            log.info("✅ Generated file tree with {} folders", fileTree.size());
            return Map.of("structure", fileTree);

        } catch (Exception e) {
            log.error("❌ Error generating file tree for admin", e);
            return Map.of("structure", new ArrayList<>(), "error", "Failed to load file tree");
        }
    }
}