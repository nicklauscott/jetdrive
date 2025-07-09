package com.niclauscott.jetdrive.file_feature.upload.service;

import com.niclauscott.jetdrive.common.model.UserPrincipal;
import com.niclauscott.jetdrive.file_feature.common.constant.FileStorageProperties;
import com.niclauscott.jetdrive.file_feature.common.exception.CantUploadFileException;
import com.niclauscott.jetdrive.file_feature.download.service.MinioService;
import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileNodeDTO;
import com.niclauscott.jetdrive.file_feature.file.service.FileNodeService;
import com.niclauscott.jetdrive.file_feature.file.service.MimeTypeUtil;
import com.niclauscott.jetdrive.file_feature.upload.exception.UncompletedUploadException;
import com.niclauscott.jetdrive.file_feature.upload.exception.UploadNotSupportedException;
import com.niclauscott.jetdrive.file_feature.upload.exception.UploadSessionNotFoundException;
import com.niclauscott.jetdrive.file_feature.upload.model.dtos.UploadProgressResponse;
import com.niclauscott.jetdrive.file_feature.upload.model.dtos.UploadInitiateRequest;
import com.niclauscott.jetdrive.file_feature.upload.model.dtos.UploadInitiateResponse;
import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadSession;
import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadStatus;
import com.niclauscott.jetdrive.file_feature.upload.repository.UploadSessionRepository;
import com.niclauscott.jetdrive.user_feature.repository.UserRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@AllArgsConstructor
public class UploadService {

    private final UploadSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final FileNodeService fileNodeService;
    private final MinioClient minioClient;
    private final MinioService minioService;
    private final FileStorageProperties fileStorageProperties;

    public String uploadProfilePicture(MultipartFile file, String userId, String oldProfilePicture)
            throws IOException {
        String mimeType = MimeTypeUtil.getMimeTypeByExtension(Objects.requireNonNull(file.getOriginalFilename()));
        if (!Objects.equals(mimeType.split("/")[0], "image")) {
            throw new CantUploadFileException("File type not supported");
        }
        String format = mimeType.split("/")[1];
        String compressedObjectName = userId.replace(".", "_") + "." + format;

        // Convert MultipartFile to BufferedImage
        BufferedImage image = ImageIO.read(file.getInputStream());

        // Compress image to ByteArrayOutputStream
        ByteArrayOutputStream compressedOut = new ByteArrayOutputStream();
        Thumbnails.of(image)
                .scale(1.0)
                .outputQuality(0.5)
                .outputFormat(format)
                .toOutputStream(compressedOut);

        byte[] compressedBytes = compressedOut.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedBytes);

        // delete the old picture
        if (oldProfilePicture != null) minioService.deleteProfilePicture(oldProfilePicture);

        // Upload to MinIO
        minioService.uploadProfilePicture(inputStream, compressedBytes.length, -1, compressedObjectName);
        return "profile-picture/" + compressedObjectName;
    }

    @Transactional
    public UploadInitiateResponse initiateUpload(UploadInitiateRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        int fileSizeMb = (int) ((int) Math.ceil(request.getFileSize()) / (1024.0 * 1024.0));
        if (!userRepository.canUserUpload(userPrincipal.getUserId(), fileSizeMb)) {
            throw new CantUploadFileException("You can't upload at the moment");
        }

        UploadSession session = saveUploadSession(
                userPrincipal.getUserId(),
                request.getFileName(), request.getFileSize(),
                request.getParentId(), request.isHasThumbnail()
        );

        String objectName = session.getId() + "/" + session.getFileName();
        String uploadId = UUID.randomUUID().toString();

        session.setObjectKey(objectName);
        session.setMinioUploadId(uploadId);
        return new UploadInitiateResponse(session.getId(), fileStorageProperties.chunkSize());
    }

    public UploadProgressResponse handleChunks(
            UUID uploadId, String contentRange, InputStream inputStream
    ) throws IOException {
        UploadSession session = sessionRepository.findById(uploadId).orElseThrow(() ->
                new UploadSessionNotFoundException("No session with upload id found"));

        if (session.getStatus().equals(UploadStatus.CANCELLED) || session.getStatus().equals(UploadStatus.COMPLETED)) {
            throw new UploadNotSupportedException("Can't resume upload");
        }

        Range range = parseContentRange(contentRange);
        int partNumber = (int) (range.start / fileStorageProperties.chunkSize()) + 1;

        if (session.getUploadedParts().containsKey(partNumber)) {
            return getUploadProgress(uploadId);
        }

        String eTag = UUID.randomUUID().toString();
        try {
            log.info("Saving new chunk: part: {}", partNumber);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(fileStorageProperties.uploadBucket())
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

    public FileNodeDTO completeUpload(UUID uploadId) {
        UploadSession session = sessionRepository.findById(uploadId).orElseThrow(() ->
                new UploadSessionNotFoundException("No session with upload id found"));

        if (session.getStatus().equals(UploadStatus.CANCELLED) || session.getStatus().equals(UploadStatus.COMPLETED)) {
            throw new UploadNotSupportedException("Upload already completed or cancelled");
        }

        if (session.getUploadedSize() < session.getTotalSize()) {
            throw new UncompletedUploadException("Missing part of the file");
        }

        String finalObject = session.getObjectKey();

        try (ByteArrayOutputStream combined = new ByteArrayOutputStream()) {
            for (int i : session.getUploadedParts().keySet().stream().sorted().toList()) {
                try (InputStream in = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(fileStorageProperties.uploadBucket())
                                .object(finalObject + ".part" + i)
                                .build()
                )) {
                    in.transferTo(combined);
                }
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(fileStorageProperties.uploadBucket())
                            .object(finalObject)
                            .stream(new ByteArrayInputStream(combined.toByteArray()), combined.size(), -1)
                            .build()
            );

            for (int i : session.getUploadedParts().keySet()) {
                try {
                    minioClient.removeObject(
                            io.minio.RemoveObjectArgs.builder()
                                    .bucket(fileStorageProperties.uploadBucket())
                                    .object(finalObject + ".part" + i)
                                    .build()
                    );
                } catch (Exception e) {
                    log.info("Failed to delete chunk part {}  {}", i, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to assemble and complete upload", e);
        }

        updateSession(uploadId);
        return fileNodeService.createFileNode(
                session.getFileName(), "file", session.getParentId(), session.getTotalSize(),
                session.getObjectKey(), session.isHasThumbnail(), session.getThumbnailPath()
        );
    }

    public UploadProgressResponse getUploadProgress(UUID uploadId) {
        UploadSession session = sessionRepository.findById(uploadId).orElseThrow(() ->
                new UploadSessionNotFoundException("No session with upload id found"));

        long uploadedBytes = session.getUploadedParts().keySet().stream()
                .mapToLong(partNumber -> {
                    long start = (partNumber - 1L) * fileStorageProperties.chunkSize();
                    long end = Math.min(start + fileStorageProperties.chunkSize(), session.getTotalSize());
                    return end - start;
                })
                .sum();

        return new UploadProgressResponse(
                new ArrayList<>(session.getUploadedParts().keySet()),
                session.getTotalSize(),
                uploadedBytes,
                fileStorageProperties.chunkSize(),
                session.getStatus()
        );
    }

    private void updateSession(UUID uploadId) {
        UploadSession session = sessionRepository.findById(uploadId).orElseThrow(() ->
                new UploadSessionNotFoundException("Not session with upload id found"));
        session.setStatus(UploadStatus.COMPLETED);
        sessionRepository.save(session);
    }

    private UploadSession saveUploadSession(
            UUID userId, String fileName, long totalSize,
            String parentId, boolean hasThumbnail
    ) {
        UploadSession uploadSession = new UploadSession();
        uploadSession.setUserId(userId);
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
