package com.tradinginfo.backend.mapper;

import com.tradinginfo.backend.dto.LessonDTO;
import com.tradinginfo.backend.entity.Lesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    LessonMapper INSTANCE = Mappers.getMapper(LessonMapper.class);

    LessonDTO toDTO(Lesson lesson);

    @Mapping(target = "fileHash", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isFolder", ignore = true)
    @Mapping(target = "subscriptionRequired", ignore = true)
    Lesson toEntity(LessonDTO lessonDTO);
}