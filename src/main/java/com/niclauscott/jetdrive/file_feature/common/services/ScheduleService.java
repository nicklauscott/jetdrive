package com.niclauscott.jetdrive.file_feature.common.services;

import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadSession;
import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadStatus;
import com.niclauscott.jetdrive.file_feature.upload.repository.UploadSessionRepository;
import io.minio.MinioClient;
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
    private final MinioClient minioClient;
    private final String bucket = "upload";

    @Scheduled(cron = "0 0 * * * *") // runs every hour
    @Transactional
    public void cleanupStaleUploads() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);

        List<UploadSession> staleSessions = sessionRepository
                .findByStatusAndLastUpdatedAtBefore(UploadStatus.IN_PROGRESS, cutoff);

        for (UploadSession session : staleSessions) {
            String finalObject = session.getObjectKey();
            for (int i : session.getUploadedParts().keySet()) {
                try {
                    minioClient.removeObject(
                            io.minio.RemoveObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(finalObject + ".part" + i)
                                    .build()
                    );
                } catch (Exception e) {
                    log.info("Failed to delete chunk part {}  {}", i, e.getMessage());
                }
            }

            session.setStatus(UploadStatus.CANCELLED);
            sessionRepository.save(session);
        }

        log.info("Cleaned up {} stale upload sessions", staleSessions.size());
    }

}