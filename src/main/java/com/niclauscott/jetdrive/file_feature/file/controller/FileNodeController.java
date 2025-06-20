package com.niclauscott.jetdrive.file_feature.file.controller;

import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileNodeTreeResponse;
import com.niclauscott.jetdrive.file_feature.file.service.FileNodeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@Tag(name = "File Node", description = "Api for managing file nodes")
@AllArgsConstructor
public class FileNodeController {

    private final FileNodeService service;

    @GetMapping
    public ResponseEntity<?> getRootFiles() {
        Optional<FileNodeTreeResponse> response = service.getFiles();

        return response
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_MODIFIED).build());
    }

    @GetMapping("/{parentId}/children")
    public ResponseEntity<?> getChildren(
            @PathVariable("parentId") UUID parentId, @RequestParam Optional<LocalDateTime> ifUpdatedSince
    ) {
        Optional<FileNodeTreeResponse> response = service.getChildren(parentId, ifUpdatedSince);

        return response
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_MODIFIED).build());
    }

}
