package com.niclauscott.jetdrive.file_feature.file.repository;

import com.niclauscott.jetdrive.file_feature.file.model.entities.FileNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileNodeRepository extends JpaRepository<FileNode, UUID> {

    @Query(value = """
            SELECT CASE WHEN MAX(updated_at) > :timestamp THEN TRUE
            ELSE FALSE END
            FROM file_node
            WHERE parent_id = :parentId AND user_id = :userId
            """, nativeQuery = true)
    boolean hasUpdatesSince(
            @Param("userId") UUID userId,
            @Param("parentId") UUID parentId,
            @Param("timestamp") LocalDateTime timestamp
    );

    Optional<FileNode> findByUserIdAndId(UUID userId, UUID parentId);

    List<FileNode> findByUserIdAndParentId(UUID userId, UUID parentId);

    List<FileNode> findByUserIdAndParentIdIsNull(UUID userId);

    void deleteByUserIdAndId(UUID userId, UUID id);

}
