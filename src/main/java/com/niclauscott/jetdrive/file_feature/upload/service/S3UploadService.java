package com.niclauscott.jetdrive.file_feature.upload.service;

import com.niclauscott.jetdrive.common.model.UserPrincipal;
import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileNodeDTO;
import com.niclauscott.jetdrive.file_feature.file.service.FileNodeService;
import com.niclauscott.jetdrive.file_feature.upload.exception.UncompletedUploadException;
import com.niclauscott.jetdrive.file_feature.upload.exception.UploadNotSupportedException;
import com.niclauscott.jetdrive.file_feature.upload.exception.UploadSessionNotFoundException;
import com.niclauscott.jetdrive.file_feature.upload.model.dtos.S3UploadProgressResponse;
import com.niclauscott.jetdrive.file_feature.upload.model.dtos.UploadInitiateRequest;
import com.niclauscott.jetdrive.file_feature.upload.model.dtos.UploadInitiateResponse;
import com.niclauscott.jetdrive.file_feature.upload.model.dtos.UploadProgressResponse;
import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadSession;
import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadStatus;
import com.niclauscott.jetdrive.file_feature.upload.repository.UploadSessionRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Log
@AllArgsConstructor
public class S3UploadService {

    private final UploadSessionRepository sessionRepository;
    private final FileNodeService fileNodeService;
    private final MinioClient minioClient;

    private final String bucket = "upload";
    private final int chunkSize = 1024 * 1024;

    @Transactional
    public UploadInitiateResponse initiateUpload(UploadInitiateRequest request) {
        UploadSession session = saveUploadSession(
                request.getFileName(), request.getFileSize(),
                request.getParentId(), request.isHasThumbnail()
        );

        String objectName = session.getId() + "/" + session.getFileName();
        String uploadId = UUID.randomUUID().toString();

        session.setObjectKey(objectName);
        session.setMinioUploadId(uploadId);
        return new UploadInitiateResponse(session.getId(), chunkSize);
    }

    public S3UploadProgressResponse handleChunks(
            UUID uploadId, String contentRange, InputStream inputStream
    ) throws IOException {
        UploadSession session = sessionRepository.findById(uploadId).orElseThrow(() ->
                new UploadSessionNotFoundException("No session with upload id found"));

        if (session.getStatus().equals(UploadStatus.CANCELLED) || session.getStatus().equals(UploadStatus.COMPLETED)) {
            throw new UploadNotSupportedException("Can't resume upload");
        }

        Range range = parseContentRange(contentRange);
        int partNumber = (int) (range.start / chunkSize) + 1;

        if (session.getUploadedParts().containsKey(partNumber)) {
            log.info("Skipping already uploaded chunk: part");
            return getUploadProgress(uploadId);
        }

        String eTag = UUID.randomUUID().toString();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(session.getObjectKey() + ".part" + partNumber)
                            .stream(inputStream, range.end - range.start + 1, -1)
                            .build()
            );
        } catch (Exception e) {
            throw new IOException("Failed to upload chunk to Minio", e);
        }

        session.getUploadedParts().put(partNumber, eTag);
        session.setUploadedSize(session.getUploadedSize() + (range.end - range.start + 1));
        session.setLastUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        return getUploadProgress(uploadId);
    }

    public FileNodeDTO completeUpload(UUID uploadId) throws IOException {
        UploadSession session = sessionRepository.findById(uploadId).orElseThrow(() ->
                new UploadSessionNotFoundException("No session with upload id found"));

        if (session.getStatus().equals(UploadStatus.CANCELLED) || session.getStatus().equals(UploadStatus.COMPLETED)) {
            throw new UploadNotSupportedException("Upload already completed");
        }

        if (session.getUploadedSize() < session.getTotalSize()) {
            throw new UncompletedUploadException("Missing part of the file");
        }

        String finalObject = session.getObjectKey();

        try (ByteArrayOutputStream combined = new ByteArrayOutputStream()) {
            for (int i : session.getUploadedParts().keySet().stream().sorted().toList()) {
                try (InputStream in = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucket)
                                .object(finalObject + ".part" + i)
                                .build()
                )) {
                    in.transferTo(combined);
                }
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(finalObject)
                            .stream(new ByteArrayInputStream(combined.toByteArray()), combined.size(), -1)
                            .build()
            );

            for (int i : session.getUploadedParts().keySet()) {
                try {
                    minioClient.removeObject(
                            io.minio.RemoveObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(finalObject + ".part" + i)
                                    .build()
                    );
                } catch (Exception e) {
                    log.info("Failed to delete chunk part " + i + " " + " " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to assemble and complete upload", e);
        }

        updateSession(uploadId, UploadStatus.COMPLETED);
        String mimeType = getFileExtension(session.getFileName());
        return fileNodeService.createFileNode(
                session.getFileName(), "file", session.getParentId(), session.getTotalSize(),
                session.getObjectKey(), mimeType, session.isHasThumbnail(), session.getThumbnailPath()
        );
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        return lastDot == -1 ? "" : fileName.substring(lastDot + 1);
    }

    public S3UploadProgressResponse getUploadProgress(UUID uploadId) {
        UploadSession session = sessionRepository.findById(uploadId).orElseThrow(() ->
                new UploadSessionNotFoundException("No session with upload id found"));

        long uploadedBytes = session.getUploadedParts().keySet().stream()
                .mapToLong(partNumber -> {
                    long start = (partNumber - 1L) * chunkSize;
                    long end = Math.min(start + chunkSize, session.getTotalSize());
                    return end - start;
                })
                .sum();

        return new S3UploadProgressResponse(
                new ArrayList<>(session.getUploadedParts().keySet()),
                session.getTotalSize(),
                uploadedBytes,
                chunkSize
        );
    }


    private void updateSession(UUID uploadId, UploadStatus status) {
        UploadSession session = sessionRepository.findById(uploadId).orElseThrow(() ->
                new UploadSessionNotFoundException("Not session with upload id found"));
        session.setStatus(status);
        sessionRepository.save(session);
    }

    private UploadSession saveUploadSession(
            String fileName, long totalSize,
            String parentId, boolean hasThumbnail
    ) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        UploadSession uploadSession = new UploadSession();
        uploadSession.setUserId(userPrincipal.getUserId());
        uploadSession.setParentId(parentId);
        uploadSession.setTotalSize(totalSize);
        uploadSession.setFileName(fileName);
        uploadSession.setHasThumbnail(hasThumbnail);
        uploadSession.setStatus(UploadStatus.IN_PROGRESS);
        return sessionRepository.save(uploadSession);
    }

    private Range parseContentRange(String header) {
        Matcher matcher = Pattern.compile("bytes (\\d+)-(\\d+)/\\d+").matcher(header);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid Content-Range header");

        long start = Long.parseLong(matcher.group(1));
        long end = Long.parseLong(matcher.group(2));
        return new Range(start, end);
    }

    private record Range(long start, long end) {}

}
