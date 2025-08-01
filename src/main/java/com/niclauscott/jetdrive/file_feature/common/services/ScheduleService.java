package com.niclauscott.jetdrive.file_feature.common.services;

import com.niclauscott.jetdrive.file_feature.file.service.S3StorageService;
import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadSession;
import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadStatus;
import com.niclauscott.jetdrive.file_feature.upload.repository.UploadSessionRepository;
//import io.minio.MinioClient;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class ScheduleService {

    private final UploadSessionRepository sessionRepository;
    private final S3StorageService storageService;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupStaleUploads() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);

        List<UploadSession> staleSessions = sessionRepository
                .findByStatusAndLastUpdatedAtBefore(UploadStatus.IN_PROGRESS, cutoff);

        for (UploadSession session : staleSessions) {
            storageService.removeIncompleteUploads(session);
            session.setStatus(UploadStatus.CANCELLED);
            sessionRepository.save(session);
        }

        log.info("Cleaned up {} stale upload sessions", staleSessions.size());
    }

}