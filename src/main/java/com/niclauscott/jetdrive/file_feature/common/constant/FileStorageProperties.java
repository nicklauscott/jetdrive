package com.niclauscott.jetdrive.file_feature.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("app.file-storage")
@Component
public record FileStorageProperties(
    String bucket,
    int chunkSize
) {
    public FileStorageProperties() {
        this("upload", 1024 * 1024);
    }
}
