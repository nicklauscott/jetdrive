package com.niclauscott.jetdrive.file_feature.file.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class FileUrlResponseDTO {
    public String url;
    public LocalDateTime expiresAt;
}