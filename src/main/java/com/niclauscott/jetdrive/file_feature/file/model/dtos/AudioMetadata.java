package com.niclauscott.jetdrive.file_feature.file.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AudioMetadata {
    String title;
    String artist;
    String genre;
    int durationInSeconds;
    String base64CoverArt;
}
