package com.tradinginfo.backend.repository;

import com.tradinginfo.backend.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    Optional<Lesson> findByPath(String path);
    List<Lesson> findByParentFolder(String parentFolder);

    @Query("SELECT l FROM Lesson l WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(l.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Lesson> searchLessons(@Param("query") String query);

    // NEW OPTIMIZED METHODS - Safe additions for performance improvement
    @Query("SELECT DISTINCT l.parentFolder FROM Lesson l WHERE l.parentFolder IS NOT NULL AND l.parentFolder != ''")
    List<String> findDistinctParentFoldersOptimized();

    @Query("SELECT l.path FROM Lesson l WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :name, '%'))")
    Optional<String> findPathByTitleContainingOptimized(@Param("name") String name);

    @Query("SELECT l FROM Lesson l WHERE l.parentFolder = :folder ORDER BY l.lessonNumber")
    List<Lesson> findByParentFolderOrderByLessonNumberOptimized(@Param("folder") String folder);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.parentFolder = :folder")
    long countByParentFolderOptimized(@Param("folder") String folder);

    @Query("SELECT l FROM Lesson l WHERE l.isFolder = true")
    List<Lesson> findAllFoldersOptimized();
}