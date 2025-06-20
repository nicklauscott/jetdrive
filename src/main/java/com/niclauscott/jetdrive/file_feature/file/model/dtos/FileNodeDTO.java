package com.niclauscott.jetdrive.file_feature.file.model.dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class FileNodeDTO {
    private UUID id;
    private String name;
    private String type;
    private UUID parentId;
    private boolean hasThumbnail;
    private String mimeType;
    private LocalDateTime updatedAt;
}
