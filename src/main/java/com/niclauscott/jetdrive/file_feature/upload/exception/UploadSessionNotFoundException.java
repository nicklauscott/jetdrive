package com.niclauscott.jetdrive.file_feature.upload.exception;

public class UploadSessionNotFoundException extends RuntimeException {
    public UploadSessionNotFoundException(String message) {
        super(message);
    }
}
