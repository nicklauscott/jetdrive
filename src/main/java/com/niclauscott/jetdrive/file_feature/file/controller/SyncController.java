package com.niclauscott.jetdrive.file_feature.file.controller;

import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileChangeEventDTO;
import com.niclauscott.jetdrive.file_feature.file.service.SyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/sync")
@Tag(name = "Sync", description = "Api for sync file nodes")
@AllArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @GetMapping("/changes")
    public ResponseEntity<List<FileChangeEventDTO>> getChanges(@RequestParam LocalDateTime since) {
        return new ResponseEntity<>(syncService.getChangeSince(since), HttpStatus.OK);
    }

}
