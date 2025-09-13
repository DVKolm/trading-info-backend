package com.tradinginfo.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "path", unique = true, nullable = false, length = 500)
    private String path;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "html_content", columnDefinition = "TEXT", nullable = false)
    private String htmlContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "frontmatter", columnDefinition = "jsonb")
    private Map<String, Object> frontmatter;

    @Column(name = "word_count")
    private Integer wordCount = 0;

    @Column(name = "parent_folder", length = 500)
    private String parentFolder;

    @Column(name = "lesson_number")
    private Integer lessonNumber;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}