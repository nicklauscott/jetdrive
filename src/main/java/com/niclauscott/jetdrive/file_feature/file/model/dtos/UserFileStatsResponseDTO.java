package com.niclauscott.jetdrive.file_feature.file.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class UserFileStatsResponseDTO {
    public long totalStorageSize;
    public long totalStorageUsed;
    public int totalFile;
    public int totalFolder;
    public double averageFileSize;
    public double largestFileSize;
    public double smallestFileSize;
    public String mostCommonMimeType;
    public LocalDateTime lastUpload;
    public List<FileNodeDTO> recentFiles;
}
