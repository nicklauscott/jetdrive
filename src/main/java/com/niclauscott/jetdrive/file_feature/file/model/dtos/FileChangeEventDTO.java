package com.niclauscott.jetdrive.file_feature.file.model.dtos;

import com.niclauscott.jetdrive.file_feature.file.model.entities.ChangeType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class FileChangeEventDTO {
    public UUID fileId;
    public UUID parentId;
    public ChangeType eventType;
    public LocalDateTime timeStamp;
    public String snapShotJson;
}
