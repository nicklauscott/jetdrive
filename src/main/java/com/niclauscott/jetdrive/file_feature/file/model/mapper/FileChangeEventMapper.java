package com.niclauscott.jetdrive.file_feature.file.model.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.niclauscott.jetdrive.file_feature.common.exception.ObjectMapperException;
import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileChangeEventDTO;
import com.niclauscott.jetdrive.file_feature.file.model.entities.FileChangeEvent;
import com.niclauscott.jetdrive.file_feature.file.model.entities.FileNode;

public class FileChangeEventMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static FileChangeEventDTO toDTO(FileChangeEvent fileChangeEvent) {
        return new FileChangeEventDTO(
                fileChangeEvent.fileId,
                fileChangeEvent.parentId,
                fileChangeEvent.eventType,
                fileChangeEvent.timeStamp,
                fileChangeEvent.snapShotJson
        );
    }

    public static String toJson(FileNode fileNode) {
        try {
            return objectMapper.writeValueAsString(fileNode);
        } catch (RuntimeException | JsonProcessingException e) {
            throw new ObjectMapperException("Failed to serialize fileNode" + e.getMessage());
        }
    }

}
