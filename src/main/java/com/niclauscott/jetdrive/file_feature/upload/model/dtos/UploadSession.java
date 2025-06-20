package com.niclauscott.jetdrive.file_feature.upload.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UploadSession {
    private UUID id;
    private UUID userId;
    private String fileName;
    private long totalSize;
}
