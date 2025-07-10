package com.niclauscott.jetdrive.file_feature.download.service;

import com.niclauscott.jetdrive.common.model.UserPrincipal;
import com.niclauscott.jetdrive.file_feature.common.exception.FileNotFoundException;
import com.niclauscott.jetdrive.file_feature.download.model.dtos.StreamVideoResource;
import com.niclauscott.jetdrive.file_feature.file.model.entities.FileNode;
import com.niclauscott.jetdrive.file_feature.file.repository.FileNodeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class DownloadService {

    private final FileNodeRepository repository;
    private final MinioService minioService;

    public StreamVideoResource serveFile(UUID fileId, String httpRangeList) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), fileId)
                .orElseThrow(() -> new FileNotFoundException("file with the id not found"));

        long fileLength = minioService.getFileLength(fileNode.getObjectId());
        long start = 0;
        long end = fileLength - 1;

        if (httpRangeList != null && httpRangeList.startsWith("bytes=")) {
            String[] ranges = httpRangeList.substring(6).split("-");
            start = Long.parseLong(ranges[0]);
            if (ranges.length > 1 && !ranges[1].isEmpty()) {
                end = Long.parseLong(ranges[1]);
            }
        }

        long contentLength = end - start + 1;
        InputStream stream = minioService.getFileStreamWithOffset(fileNode.getObjectId(), start, contentLength);
        return new StreamVideoResource(
                fileNode.getMimeType(),
                fileNode.getName(),
                new InputStreamResource(stream),
                contentLength, start, end,
                fileLength
        );
    }

}