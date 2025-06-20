package com.niclauscott.jetdrive.file_feature.upload.service;

import com.niclauscott.jetdrive.common.model.UserPrincipal;
import com.niclauscott.jetdrive.file_feature.upload.exception.UploadNotSupportedException;
import com.niclauscott.jetdrive.file_feature.upload.exception.UploadSessionNotFoundException;
import com.niclauscott.jetdrive.file_feature.upload.model.dtos.UploadInitiateResponse;
import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadSession;
import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadStatus;
import com.niclauscott.jetdrive.file_feature.upload.repository.UploadSessionRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@AllArgsConstructor
public class UploadService {

    private final UploadSessionRepository sessionRepository;

    private final Path baseTempDir = Paths.get("uploads/temp");
    private final Path baseFinalDir = Paths.get("uploads/complete");
    private final int chunkSize = 1 * 1024 * 1024;

    @Transactional
    public UploadInitiateResponse initiateUpload(
            String fileName, long totalSize
    ) {
        // Save session to DB
        UploadSession session = saveUploadSession(fileName, totalSize);
        return new UploadInitiateResponse(session.getId(), chunkSize);
    }

    public void handleChunks(
            UUID uploadId, String contentRange, InputStream inputStream
    ) throws IOException {
        // Verify that file uploadSession exist and hasn't been completed
        UploadSession session = sessionRepository.findById(uploadId).orElseThrow(() ->
                new UploadSessionNotFoundException("No session with upload id found"));

        if (session.getStatus().equals(UploadStatus.CANCELLED) || session.getStatus().equals(UploadStatus.COMPLETED)) {
            throw new UploadNotSupportedException("Can resume upload");
        }

        Range range = parseContentRange(contentRange);

        Path tempDir = baseTempDir.resolve(uploadId.toString());
        Files.createDirectories(tempDir);

        Path chunkFile = tempDir.resolve(range.start + ".part");

        long writtenBytes;
        try (OutputStream out = Files.newOutputStream(chunkFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writtenBytes = inputStream.transferTo(out);
        }

        long newUploaded = session.getUploadedSize() + writtenBytes;
        session.setUploadedSize(newUploaded);
        session.setLastUpdatedAt(LocalDateTime.now());

        sessionRepository.save(session);
    }

    public void completeUpload(UUID uploadId) throws IOException {
        UploadSession session = sessionRepository.findById(uploadId).orElseThrow(() ->
                new UploadSessionNotFoundException("No session with upload id found"));

        if (session.getStatus().equals(UploadStatus.CANCELLED) || session.getStatus().equals(UploadStatus.COMPLETED)) {
            throw new UploadNotSupportedException("Upload already completed");
        }

        Path tempDir = baseTempDir.resolve(uploadId.toString());
        Path finalFile = baseFinalDir.resolve(session.getFileName());

        Files.createDirectories(finalFile.getParent());

        try(OutputStream output = Files.newOutputStream(finalFile, StandardOpenOption.CREATE)) {
            Files.list(tempDir)
                    .sorted(Comparator.comparingLong(f ->
                            Long.parseLong(f.getFileName().toString().split("\\.")[0])))
                    .forEach(path -> {
                        try(InputStream in = Files.newInputStream(path)) {
                            in.transferTo(output);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }

        updateSession(uploadId, UploadStatus.COMPLETED);
        deleteDirectory(tempDir);
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private void updateSession(UUID uploadId, UploadStatus status) {
        UploadSession session = sessionRepository.findById(uploadId).orElseThrow(() ->
                new UploadSessionNotFoundException("Not session with upload id found"));
        session.setStatus(status);
        sessionRepository.save(session);
    }

    private Range parseContentRange(String header) {
        Matcher matcher = Pattern.compile("bytes (\\d+)-(\\d+)/\\d+").matcher(header);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid Content-Range header");

        long start = Long.parseLong(matcher.group(1));
        long end = Long.parseLong(matcher.group(2));
        return new Range(start, end);
    }

    private UploadSession saveUploadSession(String fileName, long totalSize) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        UploadSession uploadSession = new UploadSession();
        uploadSession.setUserId(userPrincipal.getUserId());
        uploadSession.setTotalSize(totalSize);
        uploadSession.setFileName(fileName);
        uploadSession.setStatus(UploadStatus.IN_PROGRESS);
        return sessionRepository.save(uploadSession);
    }

    private record Range(long start, long end) {}
}
