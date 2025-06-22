package com.niclauscott.jetdrive.file_feature.upload.repository;

import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadSession;
import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

    List<UploadSession> findByStatusAndLastUpdatedAtBefore(UploadStatus status, LocalDateTime cutoff);

}
