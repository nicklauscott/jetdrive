package com.niclauscott.jetdrive.file_feature.upload.exception;

public class UncompletedUploadException extends RuntimeException {
    public UncompletedUploadException(String message) {
        super(message);
    }
}
