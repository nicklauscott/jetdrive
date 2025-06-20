package com.niclauscott.jetdrive.file_feature.upload.model.dtos;

import lombok.Data;

@Data
public class UploadInitiateRequest {
    private String fileName;
    private long fileSize;
    private String parentId;
    private boolean hasThumbnail = false;
}
