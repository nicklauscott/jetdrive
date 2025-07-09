package com.niclauscott.jetdrive.file_feature.file.model.dtos;

import lombok.Data;

@Data
public class FileNodeMoveRequestDTO {
    private String id;
    private String newParentId;
}
