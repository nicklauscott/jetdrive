package com.niclauscott.jetdrive.file_feature.file.repository;

import com.niclauscott.jetdrive.file_feature.file.model.entities.FileChangeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FileChangeEventRepository extends JpaRepository<FileChangeEvent, UUID> {
    List<FileChangeEvent> findAllByUserIdAndTimeStampAfterOrderByTimeStampAsc(
            UUID userId, LocalDateTime since
    );
}
