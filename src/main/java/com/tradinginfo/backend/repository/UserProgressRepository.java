package com.tradinginfo.backend.repository;

import com.tradinginfo.backend.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    Optional<UserProgress> findByUserIdAndLessonPath(Long userId, String lessonPath);
    List<UserProgress> findByUserId(Long userId);
    List<UserProgress> findByUserIdAndCompleted(Long userId, boolean completed);

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.user.id = :userId AND up.completed = true")
    Long countCompletedLessonsByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(up.timeSpent) FROM UserProgress up WHERE up.user.id = :userId")
    Long getTotalTimeSpentByUserId(@Param("userId") Long userId);

    @Query("SELECT up FROM UserProgress up WHERE up.user.telegramId = :telegramId AND up.lessonPath = :lessonPath")
    Optional<UserProgress> findByTelegramId(@Param("telegramId") Long telegramId, @Param("lessonPath") String lessonPath);
}