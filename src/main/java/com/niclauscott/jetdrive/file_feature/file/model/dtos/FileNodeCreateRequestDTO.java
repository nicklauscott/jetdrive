package com.niclauscott.jetdrive.file_feature.file.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class FileNodeCreateRequestDTO {
    private String name;
    private String type;
    private String parentId;
}
