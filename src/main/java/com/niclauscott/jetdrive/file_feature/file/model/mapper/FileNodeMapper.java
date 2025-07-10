package com.niclauscott.jetdrive.file_feature.file.model.mapper;

import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileNodeDTO;
import com.niclauscott.jetdrive.file_feature.file.model.entities.FileNode;
import jakarta.transaction.Transactional;

public class FileNodeMapper {
    public static FileNodeDTO toDTO(FileNode fileNode) {
        FileNodeDTO dto = new FileNodeDTO();
        dto.setId(fileNode.getId());
        dto.setName(fileNode.getName());
        dto.setType(fileNode.getType());
        dto.setSize(fileNode.getSize());
        dto.setParentId(fileNode.getParentId());
        dto.setHasThumbnail(fileNode.getHasThumbnail());
        dto.setMimeType(fileNode.getMimeType());
        dto.setUpdatedAt(fileNode.getUpdatedAt());
        dto.setCreatedAt(fileNode.getCreatedAt());
        return dto;
    }

    @Transactional
    public static FileNode createCopy(FileNode fileNode) {
        FileNode dto = new FileNode();
        dto.setUserId(fileNode.getUserId());
        dto.setName(fileNode.getName());
        dto.setType(fileNode.getType());
        dto.setParentId(fileNode.getParentId());
        dto.setSize(fileNode.getSize());
        dto.setMimeType(fileNode.getMimeType());
        dto.setHasThumbnail(fileNode.getHasThumbnail());
        dto.setObjectId(fileNode.getObjectId());
        dto.setHasThumbnail(fileNode.getHasThumbnail());
        return dto;
    }
}
