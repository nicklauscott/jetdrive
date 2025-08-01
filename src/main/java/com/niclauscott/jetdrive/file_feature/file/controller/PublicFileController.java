package com.niclauscott.jetdrive.file_feature.file.controller;

import com.niclauscott.jetdrive.file_feature.file.service.S3StorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
@Tag(name = "Public", description = "Api for managing public resource")
@AllArgsConstructor
public class PublicFileController {

    private final S3StorageService storageService;

    @GetMapping("/profile-picture/{objectName}")
    public ResponseEntity<InputStreamResource> serveProfilePicture(@PathVariable String objectName) {
        var s3Object = storageService.serveProfilePicture(objectName);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .contentLength(s3Object.response().contentLength())
                .body(new InputStreamResource(s3Object));
    }

}
