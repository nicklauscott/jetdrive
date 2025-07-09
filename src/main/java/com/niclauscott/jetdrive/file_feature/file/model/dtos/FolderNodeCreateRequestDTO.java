package com.niclauscott.jetdrive.file_feature.file.model.dtos;

import lombok.Data;

@Data
public class FolderNodeCreateRequestDTO {
    private String name;
    private String parentId;
}
