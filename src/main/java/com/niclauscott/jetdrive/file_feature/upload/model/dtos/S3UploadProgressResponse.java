package com.niclauscott.jetdrive.file_feature.upload.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
public class S3UploadProgressResponse {
    private List<Integer> uploadedChunks;
    private long totalBytes;
    private long uploadedBytes;
    private int chunkSize;
}
