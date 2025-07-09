package com.niclauscott.jetdrive.file_feature.file.controller;

import com.niclauscott.jetdrive.file_feature.file.model.dtos.*;
import com.niclauscott.jetdrive.file_feature.file.service.FileNodeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
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
    public ResponseEntity<UserFileStatsResponseDTO> getStats() {
        return ResponseEntity.ok(service.getUserStats());
    }

    @GetMapping
    public ResponseEntity<?> getRootFiles2() {
        Optional<FileNodeTreeResponse> response = service.getFiles();

        return response
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_MODIFIED).build());
    }

    @GetMapping("/{file_id}")
    public ResponseEntity<FileNodeDTO> getFile(@PathVariable("file_id") UUID fileId) {
        FileNodeDTO response = service.getFile(fileId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{parent_Id}/children")
    public ResponseEntity<?> getChildren(
            @PathVariable("parent_Id") UUID parentId, @RequestParam Optional<LocalDateTime> ifUpdatedSince
    ) {
        Optional<FileNodeTreeResponse> response = service.getChildren(parentId, ifUpdatedSince);

        return response
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_MODIFIED).build());
    }

    @GetMapping("/search/{search_query}")
    public ResponseEntity<?> search(@PathVariable("search_query") String searchQuery) {
        Optional<FileNodeTreeResponse> response = service.search(searchQuery);
        return response
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.OK).build());
    }

    @PostMapping("/create")
    public ResponseEntity<FileNodeDTO> createFileNode(@RequestBody FileNodeCreateRequestDTO request) {
        FileNodeDTO response = service.createFileNode(request.getName(), "folder", request.getParentId(),
                0L, null, false, null);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping("/rename")
    public ResponseEntity<?> updateFileNode(@RequestBody FileNodeRenameRequestDTO request) {
       service.renameFileNode(request.getId(), request.getNewName());
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PatchMapping("/copy")
    public ResponseEntity<?> copyFileNode(@RequestBody FileNodeCopyRequestDTO request) {
        FileNodeDTO response = service.copyFileNode(request.getId(), request.getParentId());
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @PatchMapping("/move")
    public ResponseEntity<?> moveFileNode(@RequestBody FileNodeMoveRequestDTO request) {
        FileNodeDTO response= service.moveFileNode(request.getId(), request.getNewParentId());
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteFileNode(@PathVariable("id") UUID id) {
        service.deleteFileNode(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
