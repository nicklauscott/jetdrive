package com.niclauscott.jetdrive.file_feature.upload.controller;

import com.niclauscott.jetdrive.file_feature.upload.model.dtos.UploadInitiateRequest;
import com.niclauscott.jetdrive.file_feature.upload.model.dtos.UploadInitiateResponse;
import com.niclauscott.jetdrive.file_feature.upload.service.UploadService;
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
public class UploadController {

    private final UploadService service;

    @PostMapping("/initiate")
    public ResponseEntity<UploadInitiateResponse> initiateUpload(
            @RequestBody UploadInitiateRequest request
    ) {
        UploadInitiateResponse response = service.initiateUpload(request.getFileName(), request.getFileSize());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{uploadId}")
    public ResponseEntity<Void> uploadChunk(
            @PathVariable("uploadId")UUID uploadId,
            @RequestHeader("Content-Range") String contentRange,
            HttpServletRequest request
    ) throws IOException {
        log.info("Temp directory resolved to: {} -> uploadChunk", uploadId);

        service.handleChunks(uploadId, contentRange, request.getInputStream());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{uploadId}/complete")
    public  ResponseEntity<Void> completeUpload(@PathVariable("uploadId") UUID uploadId) throws IOException {
        service.completeUpload(uploadId);
        return ResponseEntity.ok().build();
    }
}
