package com.niclauscott.jetdrive.file_feature.upload.controller;

import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileNodeDTO;
import com.niclauscott.jetdrive.file_feature.upload.model.dtos.S3UploadProgressResponse;
import com.niclauscott.jetdrive.file_feature.upload.model.dtos.UploadInitiateRequest;
import com.niclauscott.jetdrive.file_feature.upload.model.dtos.UploadInitiateResponse;
import com.niclauscott.jetdrive.file_feature.upload.model.dtos.UploadProgressResponse;
import com.niclauscott.jetdrive.file_feature.upload.service.S3UploadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/upload")
@Tag(name = "Upload", description = "Api to manage file uploads")
@AllArgsConstructor
public class S3UploadController {

    private final S3UploadService service;

    @PostMapping("/initiate")
    public ResponseEntity<UploadInitiateResponse> initiateUpload(
            @RequestBody UploadInitiateRequest request
    ) {
        UploadInitiateResponse response = service.initiateUpload(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{uploadId}")
    public ResponseEntity<S3UploadProgressResponse> uploadChunk(
            @PathVariable("uploadId")UUID uploadId,
            @RequestHeader("Content-Range") String contentRange,
            HttpServletRequest request
    ) throws IOException {
        S3UploadProgressResponse response = service.handleChunks(uploadId, contentRange, request.getInputStream());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{uploadId}")
    public S3UploadProgressResponse getUploadProgress(
            @PathVariable("uploadId")UUID uploadId
    ) {
        return service.getUploadProgress(uploadId);
    }

    @PostMapping("/{uploadId}/complete")
    public  ResponseEntity<FileNodeDTO> completeUpload(@PathVariable("uploadId") UUID uploadId) throws IOException {
        return ResponseEntity.ok(service.completeUpload(uploadId));
    }
}
