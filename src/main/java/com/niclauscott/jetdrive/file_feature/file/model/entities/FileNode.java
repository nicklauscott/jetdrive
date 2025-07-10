package com.niclauscott.jetdrive.file_feature.file.model.entities;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_node")
@Data
public class FileNode {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column
    private Long size = 0L;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "has_thumbnail")
    private Boolean hasThumbnail = false;

    @Column(name = "object_id")
    private String objectId;

    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
