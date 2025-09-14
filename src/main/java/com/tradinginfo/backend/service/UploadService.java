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
            extractZipFile(file, tempDir);

            Files.walk(tempDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md"))
                    .forEach(mdFile -> processMarkdownFile(mdFile, targetFolder, uploadedFiles, errors));

            deleteDirectory(tempDir);

            result.put("success", true);
            result.put("filesUploaded", uploadedFiles.size());
            result.put("files", uploadedFiles);
            if (!errors.isEmpty()) {
                result.put("errors", errors);
            }

            sendTelegramNotificationForUpload(targetFolder, uploadedFiles);

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
            sendTelegramNotificationForSingleLessonUpload(originalFilename, targetFolder);

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

        sendTelegramNotificationForFolderDeletion(folder, lessons.size());
    }

    public void deleteSingleLesson(String lessonPath, Long telegramId) {
        Lesson lesson = lessonRepository.findByPath(lessonPath)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonPath));

        deletePhysicalFileSafely(lessonPath);
        lessonRepository.delete(lesson);
        log.info("Deleted single lesson: {}", lessonPath);

        sendTelegramNotificationForLessonDeletion(lessonPath, lesson.getTitle());
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

    private void sendTelegramNotificationForUpload(String targetFolder, List<String> uploadedFiles) {
        if (uploadedFiles.isEmpty() || telegramBotService.isEmpty()) {
            return;
        }

        try {
            String message = String.format("""
                📚 *Новые уроки загружены!*

                Папка: `%s`
                Количество файлов: %d
                Файлы: %s
                """, targetFolder, uploadedFiles.size(), String.join(", ", uploadedFiles));

            telegramBotService.get().sendMessageToChannel(message, "Markdown");
            log.info("Telegram notification sent for uploaded lessons");
        } catch (Exception e) {
            log.warn("Failed to send Telegram notification for upload", e);
        }
    }

    private void sendTelegramNotificationForFolderDeletion(String folder, int lessonsCount) {
        if (lessonsCount == 0 || telegramBotService.isEmpty()) {
            return;
        }

        try {
            String message = String.format("""
                🗑️ *Папка с уроками удалена*

                Папка: `%s`
                Удалено уроков: %d
                """, folder, lessonsCount);

            telegramBotService.get().sendMessageToChannel(message, "Markdown");
            log.info("Telegram notification sent for folder deletion");
        } catch (Exception e) {
            log.warn("Failed to send Telegram notification for folder deletion", e);
        }
    }

    private void sendTelegramNotificationForLessonDeletion(String lessonPath, String lessonTitle) {
        telegramBotService.ifPresent(botService -> {
            try {
                String message = String.format("""
                    🗑️ *Урок удален*

                    Файл: `%s`
                    Название: `%s`
                    """, lessonPath, lessonTitle);

                botService.sendMessageToChannel(message, "Markdown");
                log.info("Telegram notification sent for single lesson deletion");
            } catch (Exception e) {
                log.warn("Failed to send Telegram notification for lesson deletion", e);
            }
        });
    }

    private void sendTelegramNotificationForSingleLessonUpload(String fileName, String targetFolder) {
        telegramBotService.ifPresent(botService -> {
            try {
                String folderName = Optional.ofNullable(targetFolder).orElse("Корень");
                String message = String.format("""
                    📝 *Новый урок загружен!*

                    Файл: `%s`
                    Папка: `%s`
                    """, fileName, folderName);

                botService.sendMessageToChannel(message, "Markdown");
                log.info("Telegram notification sent for single lesson upload");
            } catch (Exception e) {
                log.warn("Failed to send Telegram notification for single lesson upload", e);
            }
        });
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
}