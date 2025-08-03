package com.niclauscott.jetdrive.file_feature.upload.model.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "upload_sessions")
@Data
public class UploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID UserId;

    @Column(name = "parent_id")
    private String parentId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "total_size", nullable = false)
    private Long totalSize;

    @Column(name = "uploaded_size", nullable = false)
    private Long uploadedSize = 0L;

    @Column(name = "has_thumbnail")
    private boolean hasThumbnail = false;

    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UploadStatus status;

    @Column(name = "object_key")
    private String objectKey;

    @Column(name = "s3_upload_id")
    private String s3UploadId;

    @ElementCollection
    @Column(name = "uploaded_chunks")
    private Set<Long> uploadedChunks = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "upload_parts", joinColumns = @JoinColumn(name = "session_id"))
    @MapKeyColumn(name = "part_number")
    @Column(name = "etag")
    private Map<Integer, String> uploadedParts = new HashMap<>();

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

}



