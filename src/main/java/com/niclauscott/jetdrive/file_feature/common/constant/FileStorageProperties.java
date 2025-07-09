package com.niclauscott.jetdrive.file_feature.common.constant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("app.file-storage")
@Component
public record FileStorageProperties(
    String uploadBucket,
    int chunkSize,
    String profileBucket
) {
    public FileStorageProperties() {
        this(
                "upload",
                1024 * 1024,
                "profile-picture"
        );
    }
}
