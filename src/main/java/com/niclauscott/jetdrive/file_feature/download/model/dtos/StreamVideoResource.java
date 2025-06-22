package com.niclauscott.jetdrive.file_feature.download.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.io.Resource;

@Data
@AllArgsConstructor
public class StreamVideoResource {
    private String mimeType;
    private String filename;
    private Resource resource;
    private long contentLength;
    private long start;
    private long end;
    private long fileSize;
}
