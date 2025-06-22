package com.niclauscott.jetdrive.file_feature.download.controller;

import com.niclauscott.jetdrive.file_feature.download.model.dtos.StreamVideoResource;
import com.niclauscott.jetdrive.file_feature.download.service.DownloadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("files/download")
@CrossOrigin(origins = "*")
@Tag(name = "Download", description = "Api to manage file downloads")
@AllArgsConstructor
public class DownloadController {

    private DownloadService service;

    @GetMapping("/{file_id}")
    public ResponseEntity<Resource> serverFile(
            @PathVariable("file_id") UUID fileID,
            @RequestParam(defaultValue = "preview") String mode,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) {

        StreamVideoResource resource = service.serveFile(fileID, mode, rangeHeader);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(resource.getMimeType()));
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

        if ("download".equals(mode)) {
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(resource.getFilename())
                    .build());
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.getFileSize())
                    .body(resource.getResource());
        }

        if ("stream".equals(mode)) {
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .header(HttpHeaders.CONTENT_TYPE, "video/" + resource.getMimeType())
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.getContentLength()))
                    .header(HttpHeaders.CONTENT_RANGE,
                            "bytes " + resource.getStart() + "-" + resource.getEnd() + "/" + resource.getFileSize())
                    .body(resource.getResource());
        }

        headers.setContentDisposition(ContentDisposition.inline()
                .filename(resource.getFilename())
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.getFileSize())
                .body(resource.getResource());

    }

}