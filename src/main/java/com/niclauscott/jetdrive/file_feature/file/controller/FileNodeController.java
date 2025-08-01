package com.niclauscott.jetdrive.file_feature.file.controller;

import com.niclauscott.jetdrive.file_feature.file.model.dtos.*;
import com.niclauscott.jetdrive.file_feature.file.service.FileNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class FileNodeController {

    private final FileNodeService service;

    @GetMapping("/stats")
    @Operation(description = "Get user file stats")
    public ResponseEntity<UserFileStatsResponseDTO> getStats() {
        return ResponseEntity.ok(service.getUserStats());
    }

    @GetMapping
    public ResponseEntity<?> getRootFiles() {
        Optional<FileNodeTreeResponse> response = service.getFiles();
        return response
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_MODIFIED).build());
    }

    @GetMapping("/{file_id}")
    @Operation(description = "Get files")
    public ResponseEntity<FileNodeDTO> getFile(@PathVariable("file_id") UUID fileId) {
        FileNodeDTO response = service.getFile(fileId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/url/{file_id}")
    @Operation(description = "Get files url")
    public ResponseEntity<FileUrlResponseDTO> getFileUrl(@PathVariable("file_id") UUID fileId) {
        FileUrlResponseDTO response = service.getFileUrl(fileId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{file_id}/metadata")
    @Operation(description = "Get audio metadata")
    public ResponseEntity<AudioMetadata> getMetadata(@PathVariable("file_id") UUID fileId) {
        AudioMetadata response = service.getMetadata(fileId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{parent_Id}/children")
    @Operation(description = "Get file node children")
    public ResponseEntity<?> getChildren(
            @PathVariable("parent_Id") UUID parentId, @RequestParam Optional<LocalDateTime> ifUpdatedSince
    ) {
        Optional<FileNodeTreeResponse> response = service.getChildren(parentId, ifUpdatedSince);

        return response
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_MODIFIED).build());
    }

    @GetMapping("/search/{search_query}")
    @Operation(description = "Search files")
    public ResponseEntity<?> search(@PathVariable("search_query") String searchQuery) {
        Optional<FileNodeTreeResponse> response = service.search(searchQuery);
        return response
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.OK).build());
    }

    @PostMapping("/create")
    @Operation(description = "Create file")
    public ResponseEntity<FileNodeDTO> createFileNode(@RequestBody FileNodeCreateRequestDTO request) {
        FileNodeDTO response = service.createFileNode(request.getName(), "folder", request.getParentId(),
                0L, null, false, null);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping("/rename")
    @Operation(description = "Update file")
    public ResponseEntity<?> updateFileNode(@RequestBody FileNodeRenameRequestDTO request) {
       service.renameFileNode(request.getId(), request.getNewName());
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PatchMapping("/copy")
    @Operation(description = "Copy file")
    public ResponseEntity<?> copyFileNode(@RequestBody FileNodeCopyRequestDTO request) {
        FileNodeDTO response = service.copyFileNode(request.getId(), request.getParentId());
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @PatchMapping("/move")
    @Operation(description = "Move file")
    public ResponseEntity<?> moveFileNode(@RequestBody FileNodeMoveRequestDTO request) {
        FileNodeDTO response= service.moveFileNode(request.getId(), request.getNewParentId());
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(description = "Delete file")
    public ResponseEntity<?> deleteFileNode(@PathVariable("id") UUID id) {
        service.deleteFileNode(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
