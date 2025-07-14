package com.niclauscott.jetdrive.file_feature.file.model.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_system_changes")
@Data
public class FileChangeEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID id;

    @Enumerated(EnumType.STRING)
    public ChangeType eventType;

    public UUID userId;

    public UUID fileId;
    public UUID parentId;

    public LocalDateTime timeStamp = LocalDateTime.now();

    @Lob
    public String snapShotJson;
}
