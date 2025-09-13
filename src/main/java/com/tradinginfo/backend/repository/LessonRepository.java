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
    List<Lesson> findByParentFolderOrderByLessonNumber(String parentFolder);

    @Query("SELECT l FROM Lesson l WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(l.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Lesson> searchLessons(@Param("query") String query);

    boolean existsByPath(String path);
    void deleteByPath(String path);
}