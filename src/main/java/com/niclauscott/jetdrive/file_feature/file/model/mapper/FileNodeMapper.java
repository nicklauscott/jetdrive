package com.niclauscott.jetdrive.file_feature.file.model.mapper;

import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileNodeDTO;
import com.niclauscott.jetdrive.file_feature.file.model.entities.FileNode;

public class FileNodeMapper {
    public static FileNodeDTO toDTO(FileNode fileNode) {
        FileNodeDTO dto = new FileNodeDTO();
        dto.setId(fileNode.getId());
        dto.setName(fileNode.getName());
        dto.setType(fileNode.getType());
        dto.setParentId(fileNode.getParentId());
        dto.setHasThumbnail(fileNode.getHasThumbnail());
        dto.setMimeType(fileNode.getMimeType());
        dto.setUpdatedAt(fileNode.getUpdatedAt());
        return dto;
    }
}
