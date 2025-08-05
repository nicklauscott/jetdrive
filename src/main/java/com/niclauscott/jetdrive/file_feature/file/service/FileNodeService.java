package com.niclauscott.jetdrive.file_feature.file.service;

import com.niclauscott.jetdrive.common.model.UserPrincipal;
import com.niclauscott.jetdrive.file_feature.common.exception.FileNodeOperationException;
import com.niclauscott.jetdrive.file_feature.common.exception.FileNotFoundException;
import com.niclauscott.jetdrive.file_feature.file.model.constant.DefaultFileNodes;
import com.niclauscott.jetdrive.file_feature.file.model.dtos.*;
import com.niclauscott.jetdrive.file_feature.file.model.entities.ChangeType;
import com.niclauscott.jetdrive.file_feature.file.model.entities.FileChangeEvent;
import com.niclauscott.jetdrive.file_feature.file.model.entities.FileNode;
import com.niclauscott.jetdrive.file_feature.file.model.mapper.FileChangeEventMapper;
import com.niclauscott.jetdrive.file_feature.file.model.mapper.FileNodeMapper;
import com.niclauscott.jetdrive.file_feature.file.repository.FileChangeEventRepository;
import com.niclauscott.jetdrive.file_feature.file.repository.FileNodeRepository;
import com.niclauscott.jetdrive.user_feature.exception.UserDoesntExistException;
import com.niclauscott.jetdrive.user_feature.model.entities.User;
import com.niclauscott.jetdrive.user_feature.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class FileNodeService {
    private final FileNodeRepository repository;
    private final FileChangeEventRepository eventRepository;
    private final UserRepository userRepository;
    private final S3StorageService storageService;

    public UserFileStatsResponseDTO getUserStats() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        User user = userRepository.findById(userPrincipal.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UUID userId = userPrincipal.getUserId();
        return new UserFileStatsResponseDTO(
                (long) user.getQuotaLimitMb() * 1024 * 1024,
                repository.getTotalStorageUsed(userId),
                repository.getTotalFileCount(userId),
                repository.getTotalFolderCount(userId),
                repository.getAverageFileSize(userId),
                repository.getLargestFileSize(userId),
                repository.getSmallestFileSize(userId),
                repository.getMostCommonMimeType(userId),
                repository.getLastUploadTime(userId),
                repository.findTop5RecentFiles(userId, PageRequest.of(0, 5))
                        .stream().map(FileNodeMapper::toDTO).toList()
        );

    }

    public Optional<FileNodeTreeResponse> getFiles() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        List<FileNode> rootFiles = repository.findByUserIdAndParentIdIsNull(userPrincipal.getUserId());
        List<FileNodeDTO> dtoList = rootFiles.stream().map(FileNodeMapper::toDTO).toList();

        return Optional.of(new FileNodeTreeResponse(null, LocalDateTime.now(), dtoList));
    }

    public FileNodeDTO getFile(UUID fileId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), fileId)
                .orElseThrow(() -> new FileNotFoundException("No file with the id found"));
        return FileNodeMapper.toDTO(fileNode);
    }

    public FileUrlResponseDTO getFileUrl(UUID fileId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), fileId)
                .orElseThrow(() -> new FileNotFoundException("No file with the id found"));
        if (fileNode.getObjectId() == null) throw new FileNotFoundException("No file with the id found");
        return storageService.getPresignedUrl(fileNode.getObjectId());
    }

    public Optional<FileNodeTreeResponse> getChildren(UUID parentId, Optional<LocalDateTime> ifUpdatedSince) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        if (ifUpdatedSince.isPresent()) {
            boolean updated = repository.hasUpdatesSince(userPrincipal.getUserId(), parentId, ifUpdatedSince.get());
            if (!updated) return Optional.empty();
        }

        List<FileNode> children = repository.findByUserIdAndParentId(userPrincipal.getUserId(), parentId);
        List<FileNodeDTO> dtoList = children.stream().map(FileNodeMapper::toDTO).toList();

        LocalDateTime maxUpdated = children.stream()
                .map(FileNode::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        return Optional.of(new FileNodeTreeResponse(parentId, maxUpdated, dtoList));
    }

    public Optional<FileNodeTreeResponse> search(String searchQuery) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        List<FileNode> children = repository.searchByName(userPrincipal.getUserId(), searchQuery);
        List<FileNodeDTO> dtoList = children.stream().map(FileNodeMapper::toDTO).toList();

        LocalDateTime maxUpdated = children.stream()
                .map(FileNode::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        return Optional.of(new FileNodeTreeResponse(null, maxUpdated, dtoList));
    }

    public FileNodeDTO createFileNode(
            String name, String type, String parentId, long size,
            String storagePath, boolean hasThumbnail, String thumbnailPath
    ) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        var user = userRepository.findById(userPrincipal.getUserId())
                .orElseThrow(() -> new UserDoesntExistException("No user with the id found"));

        if (Objects.equals(type, "file")) hasEnoughSpace(userPrincipal.getUserId(), user.getQuotaLimitMb(),  size);

        FileNode fileNode = new FileNode();
        fileNode.setUserId(userPrincipal.getUserId());
        if (parentId != null) fileNode.setParentId(UUID.fromString(parentId));
        fileNode.setName(name);
        fileNode.setType(type);
        if (!Objects.equals(type, "folder")) fileNode.setMimeType(MimeTypeUtil.getMimeTypeByExtension(name));
        fileNode.setSize(size);
        fileNode.setObjectId(storagePath);
        fileNode.setHasThumbnail(hasThumbnail);
        if (thumbnailPath != null) fileNode.setThumbnailPath(thumbnailPath);

        FileNode dbFileNode = repository.save(fileNode);
        updateAllParentUpdatedAt(dbFileNode.getParentId());
        saveEvent(dbFileNode, null, ChangeType.CREATED);
        return FileNodeMapper.toDTO(fileNode);
    }

    @Transactional
    public void renameFileNode(String id, String name) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), UUID.fromString(id))
                .orElseThrow(() -> new FileNotFoundException("file with the id not found"));
        repository.renameFile(name, fileNode.getId(), userPrincipal.getUserId());
        saveEvent(fileNode,  null, ChangeType.MODIFIED);
        updateAllParentUpdatedAt(fileNode.getParentId());
    }

    @Transactional
    public void deleteFileNode(UUID id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), id)
                .orElseThrow(() -> new FileNotFoundException("file with the id not found"));

        isBelowMaxFileCount(fileNode.getId());

        UUID parentId = fileNode.getParentId();
        if (Objects.equals(fileNode.getType(), "folder")) {
            deleteFolderWithS3Cleanup(fileNode.getId());
        } else {
            storageService.deleteFile(fileNode.getObjectId());
        }

        saveEvent(fileNode, null, ChangeType.DELETED);
        repository.delete(fileNode);
        updateAllParentUpdatedAt(parentId);
    }

    @Transactional
    public FileNodeDTO copyFileNode(String id, String parentId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        var user = userRepository.findById(userPrincipal.getUserId())
                .orElseThrow(() -> new UserDoesntExistException("No user with the id found"));

        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), UUID.fromString(id))
                .orElseThrow(() -> new FileNotFoundException("file with the id not found"));

        isBelowMaxFileCount(fileNode.getId());

        hasEnoughSpace(userPrincipal.getUserId(), user.getQuotaLimitMb(), fileNode.getSize());

        FileNode newFileNode = FileNodeMapper.createCopy(fileNode);
        var newParentId = parentId != null ? UUID.fromString(parentId) : null;
        newFileNode.setParentId(newParentId);

        if (Objects.equals(newFileNode.getType(), "file")) {
            var newObjectId = UUID.randomUUID() + "/" +  fileNode.getObjectId().split("/")[1];
            storageService.copyObject(fileNode.getObjectId(), newObjectId);
            newFileNode.setObjectId(newObjectId);
        }

        FileNode dbFileNode = repository.save(newFileNode);

        if (Objects.equals(newFileNode.getType(), "folder")) {
            copyFolderTree(userPrincipal.getUserId(), fileNode.getId(), dbFileNode.getId());
        }

        // update modification date and file sync
        saveEvent(newFileNode, fileNode.getParentId(), ChangeType.MODIFIED);
        updateAllParentUpdatedAt(fileNode.getParentId());
        updateAllParentUpdatedAt(newParentId);

        return FileNodeMapper.toDTO(dbFileNode);
    }

    @Transactional
    public FileNodeDTO moveFileNode(String id, String newParentID) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), UUID.fromString(id))
                .orElseThrow(() -> new FileNotFoundException("file with the id not found"));
        fileNode.setParentId(UUID.fromString(newParentID)); // move the file node to a new parent (i.e., folder)

        // doesn't work with H2 db
        //isBelowMaxFileCount(fileNode.getId()); // uncomment later

        FileNode dbFileNode = repository.save(fileNode);
        saveEvent(dbFileNode, null, ChangeType.MOVED);
        updateAllParentUpdatedAt(fileNode.getParentId());
        updateAllParentUpdatedAt(dbFileNode.getParentId());
        return FileNodeMapper.toDTO(dbFileNode);
    }

    @Transactional
    public void deleteFolderWithS3Cleanup(UUID folderId) {
        List<FileNode> allNodes = repository.findAllDescendants(folderId);

        for (FileNode node : allNodes) {
            if (Objects.equals(node.getType(), "file")) {
                storageService.deleteFile(node.getObjectId());
            }
        }

        List<UUID> allIds = allNodes.stream().map(FileNode::getId).toList();
        repository.deleteAllByIdInBatch(allIds);
    }

    @Transactional
    public void createFileNodeForNewUser(UUID userId) {
        for (String folder: DefaultFileNodes.folderName) {
            FileNode fileNode = new FileNode();
            fileNode.setUserId(userId);
            fileNode.setName(folder);
            fileNode.setType("folder");
            repository.save(fileNode);
            saveEvent(fileNode, null, ChangeType.CREATED);
        }
    }

    @Transactional
    private void updateAllParentUpdatedAt(UUID fileNodeId) {
        UUID currentId = fileNodeId;
        while (currentId != null) {
            Optional<FileNode> optional = repository.findById(currentId);
            if (optional.isEmpty()) break;

            FileNode fileNode = optional.get();
            currentId = fileNode.getParentId();
            fileNode.setUpdatedAt(LocalDateTime.now());
            repository.save(fileNode);
        }
    }

    @Transactional
    public void copyFolderTree(UUID userId, UUID sourceFolderId, UUID newFolderId) {
        Queue<Pair<UUID, UUID>> queue = new ArrayDeque<>();
        queue.add(Pair.of(sourceFolderId, newFolderId));

        while (!queue.isEmpty()) {
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserDoesntExistException("No user with the id found"));
            Pair<UUID, UUID> current = queue.poll();
            UUID from = current.getLeft();
            UUID to = current.getRight();

            List<FileNode> children = repository.findByUserIdAndParentId(userId, from);

            for (FileNode child : children) {
                FileNode copy = FileNodeMapper.createCopy(child);
                copy.setParentId(to);

                if (Objects.equals(child.getType(), "file")) {
                    String newObjectId = UUID.randomUUID() + "/" +  child.getObjectId().split("/")[1];
                    storageService.copyObject(child.getObjectId(), newObjectId);
                    copy.setObjectId(newObjectId);
                } else { copy.setObjectId(null); }

                FileNode savedCopy = repository.save(copy);
                hasEnoughSpace(user.getId(), user.getQuotaLimitMb(), savedCopy.getSize());

                if (Objects.equals(child.getType(), "folder")) {
                    queue.add(Pair.of(child.getId(), savedCopy.getId()));
                }
            }
        }
    }

    public void deleteUserFiles(UUID userId) {
        List<FileNode> rootFiles = repository.findByUserIdAndParentIdIsNull(userId);
        for (FileNode fileNode: rootFiles) {
            deleteFolderWithS3Cleanup(fileNode.getId());
            repository.delete(fileNode);
        }
    }

    public AudioMetadata getMetadata(UUID fileId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), fileId)
                .orElseThrow(() -> new FileNotFoundException("No file with the id found"));
        if (fileNode.getObjectId() == null) throw new FileNotFoundException("No file with the id found");
        InputStream stream = storageService.getFileStream(fileNode.getObjectId());
        return AudioMetadataExtractor.extractMetadata(stream);
    }

    private void saveEvent(FileNode fileNode, UUID oldParentId, ChangeType eventType) {
        FileChangeEvent event = new FileChangeEvent();
        event.setFileId(fileNode.getId());
        event.setParentId(fileNode.getParentId());
        event.setOldParentId(oldParentId);
        event.setUserId(fileNode.getUserId());
        event.setEventType(eventType);
        event.setSnapShotJson(FileChangeEventMapper.toJson(FileNodeMapper.toDTO(fileNode)));
        eventRepository.save(event);
    }

    private void isBelowMaxFileCount(UUID fileId) {
        var descendants = repository.findAllDescendants(fileId);
        if (descendants.size() > 100) {
            throw new FileNodeOperationException("Too many items to process at once");
        }
    }

    private void hasEnoughSpace(UUID userId, long totalSpaceMb, long fileSizeBytes) {
        long totalSpaceLong = totalSpaceMb * 1024 * 1034;
        long usedSpaceBytes = repository.getTotalStorageUsed(userId);
        if (totalSpaceLong - usedSpaceBytes < fileSizeBytes) {
            throw new FileNodeOperationException("You don't have enough space to perform this action");
        }
    }
}

