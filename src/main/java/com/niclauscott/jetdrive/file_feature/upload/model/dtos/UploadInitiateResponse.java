package com.niclauscott.jetdrive.file_feature.upload.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UploadInitiateResponse {
    private UUID uploadId;
    private int chunkSize;
}
