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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
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
        Map<String, Object> result = new HashMap<>();
        List<String> uploadedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            Path tempDir = Files.createTempDirectory("upload_");
            extractZipFile(file, tempDir);

            Files.walk(tempDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md"))
                    .forEach(mdFile -> {
                        try {
                            String content = Files.readString(mdFile, StandardCharsets.UTF_8);
                            String fileName = mdFile.getFileName().toString();
                            String lessonPath = targetFolder + "/" + fileName;

                            saveLessonToDatabase(lessonPath, content, targetFolder);
                            uploadedFiles.add(fileName);
                            log.info("✅ Uploaded lesson: {}", fileName);
                        } catch (Exception e) {
                            log.error("❌ Failed to process file: {}", mdFile.getFileName(), e);
                            errors.add(mdFile.getFileName().toString() + ": " + e.getMessage());
                        }
                    });

            deleteDirectory(tempDir);

            result.put("success", true);
            result.put("filesUploaded", uploadedFiles.size());
            result.put("files", uploadedFiles);
            if (!errors.isEmpty()) {
                result.put("errors", errors);
            }

            // Send Telegram notification for successful upload
            if (!uploadedFiles.isEmpty() && telegramBotService.isPresent()) {
                try {
                    String message = String.format("📚 *Новые уроки загружены!*\n\n" +
                            "Папка: `%s`\n" +
                            "Количество файлов: %d\n" +
                            "Файлы: %s",
                            targetFolder, uploadedFiles.size(), String.join(", ", uploadedFiles));

                    telegramBotService.get().sendMessageToChannel(message, "Markdown");
                    log.info("✅ Telegram notification sent for uploaded lessons");
                } catch (Exception e) {
                    log.warn("⚠️ Failed to send Telegram notification", e);
                }
            }

        } catch (Exception e) {
            log.error("❌ Upload failed", e);
            result.put("success", false);
            result.put("error", e.getMessage());
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
        } catch (Exception e) {
            log.error("Failed to calculate hash", e);
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
        Map<String, Object> result = new HashMap<>();

        try {
            if (!file.getOriginalFilename().endsWith(".md")) {
                result.put("success", false);
                result.put("error", "Only Markdown (.md) files are allowed");
                return result;
            }

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String fileName = file.getOriginalFilename();
            String lessonPath = (targetFolder != null ? targetFolder + "/" : "") + fileName;

            saveLessonToDatabase(lessonPath, content, targetFolder);

            result.put("success", true);
            result.put("message", "Lesson uploaded successfully: " + fileName);
            result.put("fileName", fileName);

            log.info("✅ Uploaded single lesson: {}", fileName);

            // Send Telegram notification for single lesson upload
            if (telegramBotService.isPresent()) {
                try {
                    String message = String.format("📝 *Новый урок загружен!*\n\n" +
                            "Файл: `%s`\n" +
                            "Папка: `%s`",
                            fileName, targetFolder != null ? targetFolder : "Корень");

                    telegramBotService.get().sendMessageToChannel(message, "Markdown");
                    log.info("✅ Telegram notification sent for single lesson upload");
                } catch (Exception e) {
                    log.warn("⚠️ Failed to send Telegram notification", e);
                }
            }

        } catch (Exception e) {
            log.error("❌ Failed to upload single lesson", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    public void deleteLessonsFolder(String folder, Long telegramId) {
        List<Lesson> lessons = lessonRepository.findByParentFolder(folder);
        lessonRepository.deleteAll(lessons);
        log.info("✅ Deleted {} lessons from folder: {}", lessons.size(), folder);

        // Send Telegram notification for folder deletion
        if (telegramBotService.isPresent() && !lessons.isEmpty()) {
            try {
                String message = String.format("🗑️ *Папка с уроками удалена*\n\n" +
                        "Папка: `%s`\n" +
                        "Удалено уроков: %d",
                        folder, lessons.size());

                telegramBotService.get().sendMessageToChannel(message, "Markdown");
                log.info("✅ Telegram notification sent for folder deletion");
            } catch (Exception e) {
                log.warn("⚠️ Failed to send Telegram notification", e);
            }
        }
    }
}