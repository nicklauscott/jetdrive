package com.niclauscott.jetdrive.file_feature.file.repository;

import com.niclauscott.jetdrive.file_feature.file.model.entities.FileNode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query(
        """
            UPDATE FileNode f
            SET f.name = :newName
            WHERE f.id = :fileId AND f.userId = :userId
        """
    )
    void renameFile(
            @Param("newName") String newName,
            @Param("fileId") UUID fileId,
            @Param("userId") UUID userId
    );

    Optional<FileNode> findByUserIdAndId(UUID userId, UUID parentId);

    List<FileNode> findByUserIdAndParentId(UUID userId, UUID parentId);

    List<FileNode> findByUserIdAndParentIdIsNull(UUID userId);

    @Query(value = """
    SELECT * FROM file_node
    WHERE user_id = :userId AND name ILIKE %:name%
    """, nativeQuery = true)
    List<FileNode> searchByName(@Param("userId") UUID userId, @Param("name") String name);


    void deleteByUserIdAndId(UUID userId, UUID id);

    @Query(value = """
    WITH RECURSIVE descendants AS (
        SELECT * FROM file_node WHERE id = :folderId
        UNION ALL
        SELECT f.* FROM file_node f
        JOIN descendants d ON f.parent_id = d.id
    )
    SELECT * FROM descendants
    """, nativeQuery = true)
    List<FileNode> findAllDescendants(@Param("folderId") UUID folderId);


    // file stats

    // 1. Total storage used (files only)
    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileNode f WHERE f.userId = :userId AND f.type = 'file'")
    long getTotalStorageUsed(@Param("userId") UUID userId);

    // 2. Total file count
    @Query("SELECT COUNT(f) FROM FileNode f WHERE f.userId = :userId AND f.type = 'file'")
    int getTotalFileCount(@Param("userId") UUID userId);

    // 3. Total folder count
    @Query("SELECT COUNT(f) FROM FileNode f WHERE f.userId = :userId AND f.type = 'folder'")
    int getTotalFolderCount(@Param("userId") UUID userId);

    // 4. Average file size
    @Query("SELECT COALESCE(AVG(f.size), 0) FROM FileNode f WHERE f.userId = :userId AND f.type = 'file'")
    double getAverageFileSize(@Param("userId") UUID userId);

    // 5. Largest file size
    @Query("SELECT COALESCE(MAX(f.size), 0) FROM FileNode f WHERE f.userId = :userId AND f.type = 'file'")
    double getLargestFileSize(@Param("userId") UUID userId);

    // 6. Smallest file size (excluding zero-sized files if needed)
    @Query("SELECT COALESCE(MIN(f.size), 0) FROM FileNode f WHERE f.userId = :userId AND f.type = 'file'")
    double getSmallestFileSize(@Param("userId") UUID userId);

    // 7. Most common MIME type (mode)
    @Query("SELECT f.mimeType FROM FileNode f WHERE f.userId = :userId AND f.type = 'file' AND f.mimeType IS NOT NULL GROUP BY f.mimeType ORDER BY COUNT(f.mimeType) DESC LIMIT 1")
    String getMostCommonMimeType(@Param("userId") UUID userId);

    // 8. Last upload time (latest createdAt)
    @Query("SELECT MAX(f.createdAt) FROM FileNode f WHERE f.userId = :userId")
    LocalDateTime getLastUploadTime(@Param("userId") UUID userId);

    @Query("SELECT f FROM FileNode f WHERE f.userId = :userId AND f.type = 'file' ORDER BY f.createdAt DESC")
    List<FileNode> findTop5RecentFiles(@Param("userId") UUID userId, Pageable pageable);

}
