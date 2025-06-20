package com.niclauscott.jetdrive.file_feature.upload.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Set;

@Data
@AllArgsConstructor
public class UploadProgressResponse {
    private Set<Long> uploadedChunks;
    private long totalBytes;
    private long uploadedBytes;
    private int chunkSize;
}
