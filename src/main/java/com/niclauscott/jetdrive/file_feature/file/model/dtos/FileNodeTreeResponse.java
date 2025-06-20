package com.niclauscott.jetdrive.file_feature.file.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class FileNodeTreeResponse {
    private UUID parentId;
    private LocalDateTime updatedAt;
    private List<FileNodeDTO> children;
}
