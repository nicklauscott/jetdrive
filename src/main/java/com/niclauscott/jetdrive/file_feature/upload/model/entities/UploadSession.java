package com.niclauscott.jetdrive.file_feature.upload.model.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "upload_sessions")
@Data
public class UploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID UserId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "total_size", nullable = false)
    private Long totalSize;

    @Column(name = "uploaded_size", nullable = false)
    private Long uploadedSize = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UploadStatus status;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

}



