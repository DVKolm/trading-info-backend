package com.tradinginfo.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "lesson_path", nullable = false, length = 500)
    private String lessonPath;

    @Column(name = "time_spent")
    private Integer timeSpent = 0;

    @Column(name = "scroll_progress", precision = 5, scale = 2)
    private BigDecimal scrollProgress = BigDecimal.ZERO;

    @Column(name = "reading_speed", precision = 8, scale = 2)
    private BigDecimal readingSpeed = BigDecimal.ZERO;

    @Column(name = "completion_score", precision = 3, scale = 2)
    private BigDecimal completionScore = BigDecimal.ZERO;

    @Column(name = "visits")
    private Integer visits = 0;

    @Column(name = "last_visited")
    private LocalDateTime lastVisited;

    @Column(name = "engagement_level", length = 10)
    private String engagementLevel = "low";

    @Column(name = "completed")
    private Boolean completed = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}