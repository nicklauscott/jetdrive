package com.niclauscott.jetdrive.file_feature.upload.model.dtos;

import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UploadProgressResponse {
    private List<Integer> uploadedChunks;
    private long totalBytes;
    private long uploadedBytes;
    private int chunkSize;
    private UploadStatus uploadStatus;
}
