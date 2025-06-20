package com.niclauscott.jetdrive.file_feature.upload.repository;

import com.niclauscott.jetdrive.file_feature.upload.model.entities.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {
}
