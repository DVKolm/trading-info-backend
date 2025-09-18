package com.tradinginfo.backend.mapper;

import com.tradinginfo.backend.dto.UserStatisticsDTO;
import com.tradinginfo.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "lastActivity", source = "lastActive")
    @Mapping(target = "totalLessonsRead", constant = "0")
    @Mapping(target = "totalLessonsViewed", constant = "0")
    @Mapping(target = "completedLessons", constant = "0")
    @Mapping(target = "totalLessonsCompleted", constant = "0")
    @Mapping(target = "totalTimeSpent", constant = "0L")
    @Mapping(target = "averageReadingSpeed", constant = "0.0")
    @Mapping(target = "completionRate", constant = "0.0")
    @Mapping(target = "totalVisits", constant = "0")
    @Mapping(target = "currentStreak", constant = "0")
    @Mapping(target = "longestStreak", constant = "0")
    @Mapping(target = "engagementLevel", constant = "new")
    @Mapping(target = "levelProgress", expression = "java(java.util.Map.of())")
    @Mapping(target = "recentActivity", expression = "java(java.util.List.of())")
    @Mapping(target = "achievements", expression = "java(java.util.List.of())")
    UserStatisticsDTO toUserStatisticsDTO(User user);
}