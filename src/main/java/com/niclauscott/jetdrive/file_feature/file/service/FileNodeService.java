package com.niclauscott.jetdrive.file_feature.file.service;

import com.niclauscott.jetdrive.common.model.UserPrincipal;
import com.niclauscott.jetdrive.file_feature.common.exception.FileNotFoundException;
import com.niclauscott.jetdrive.file_feature.download.service.MinioService;
import com.niclauscott.jetdrive.file_feature.file.model.constant.DefaultFileNodes;
import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileNodeDTO;
import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileNodeTreeResponse;
import com.niclauscott.jetdrive.file_feature.file.model.dtos.UserFileStatsResponseDTO;
import com.niclauscott.jetdrive.file_feature.file.model.entities.FileNode;
import com.niclauscott.jetdrive.file_feature.file.model.mapper.FileNodeMapper;
import com.niclauscott.jetdrive.file_feature.file.repository.FileNodeRepository;
import com.niclauscott.jetdrive.user_feature.model.entities.User;
import com.niclauscott.jetdrive.user_feature.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@AllArgsConstructor
public class FileNodeService {

    private final FileNodeRepository repository;
    private final UserRepository userRepository;
    private final MinioService minioService;

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

    public FileNodeDTO getFile(UUID fileID) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), fileID)
                .orElseThrow(() -> new FileNotFoundException("No file with the id found"));
        return FileNodeMapper.toDTO(fileNode);
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
        FileNode fileNode = new FileNode();
        fileNode.setUserId(userPrincipal.getUserId());
        if (parentId != null) fileNode.setParentId(UUID.fromString(parentId));
        fileNode.setName(name);
        fileNode.setType(type);
        if (!Objects.equals(type, "folder")) fileNode.setMimeType(MimeTypeUtil.getMimeTypeByExtension(name));
        fileNode.setSize(size);
        fileNode.setStoragePath(storagePath);
        fileNode.setHasThumbnail(hasThumbnail);
        if (thumbnailPath != null) fileNode.setThumbnailPath(thumbnailPath);

        FileNode dbFileNode = repository.save(fileNode);
        updateAllParentUpdatedAt(dbFileNode.getParentId());
        return FileNodeMapper.toDTO(fileNode);
    }

    @Transactional
    public void renameFileNode(String id, String name) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), UUID.fromString(id))
                .orElseThrow(() -> new FileNotFoundException("file with the id not found"));
        repository.renameFile(name, fileNode.getId(), userPrincipal.getUserId());
        updateAllParentUpdatedAt(fileNode.getParentId());
    }

    @Transactional
    public void deleteFileNode(UUID id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), id)
                .orElseThrow(() -> new FileNotFoundException("file with the id not found"));

        UUID parentId = fileNode.getParentId();
        if (Objects.equals(fileNode.getType(), "folder")) {
            deleteFolderWithS3Cleanup(fileNode.getId());
        } else {
            // TODO("Delete thumbnail and the actual file from S3 uploadBucket")
        }

        repository.delete(fileNode);
        updateAllParentUpdatedAt(parentId);
    }

    @Transactional
    public FileNodeDTO copyFileNode(String id, String parentId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), UUID.fromString(id))
                .orElseThrow(() -> new FileNotFoundException("file with the id not found"));

        // create a copy of the old file node
        FileNode newFileNode = FileNodeMapper.createCopy(fileNode);
        var newParentId = parentId != null ? UUID.fromString(parentId) : null;
        newFileNode.setParentId(newParentId);
        FileNode dbFileNode = repository.save(newFileNode);

        // copy all descendants if the file is a folder
        copyFolderTree(userPrincipal.getUserId(), fileNode.getId(), dbFileNode.getId());

        // update modification date
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

        FileNode dbFileNode = repository.save(fileNode);
        updateAllParentUpdatedAt(fileNode.getParentId());
        updateAllParentUpdatedAt(dbFileNode.getParentId());
        return FileNodeMapper.toDTO(dbFileNode);
    }

    @Transactional
    public void deleteFolderWithS3Cleanup(UUID folderId) {
        List<FileNode> allNodes = repository.findAllDescendants(folderId);

        for (FileNode node : allNodes) {
            if (Objects.equals(node.getType(), "file")) {
                // minioService.deleteFile(node.getStoragePath());
            }
        }

        // Delete all metadata (can batch delete by IDs)
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
            Pair<UUID, UUID> current = queue.poll();
            UUID from = current.getLeft();
            UUID to = current.getRight();

            List<FileNode> children = repository.findByUserIdAndParentId(userId, from);

            for (FileNode child : children) {
                FileNode copy = FileNodeMapper.createCopy(child);
                copy.setParentId(to);

                // Generate new S3 path if it's a file
                if (Objects.equals(child.getType(), "file")) {
                    String newS3Path = generateNewS3Path(copy.getId(), child.getMimeType());
                    copy.setStoragePath(newS3Path);

                    //minioService.copyObject(child.getStoragePath(), newS3Path); // Copy in S3
                } else {
                    copy.setStoragePath(null);
                }

                FileNode savedCopy = repository.save(copy);

                if (Objects.equals(child.getType(), "folder")) {
                    queue.add(Pair.of(child.getId(), savedCopy.getId())); // enqueue for further copy
                }
            }
        }
    }

    public String generateNewS3Path(UUID fileId, String mimeType) {
        String extension = MimeTypeUtil.getMimeTypeByExtension(mimeType);
        return "uploads/" + fileId + (extension != null ? "." + extension : "");
    }

    public void deleteUserFiles(UUID userId) {
        List<FileNode> rootFiles = repository.findByUserIdAndParentIdIsNull(userId);
        for (FileNode fileNode: rootFiles) {
            deleteFolderWithS3Cleanup(fileNode.getId());
            repository.delete(fileNode);
        }
    }

}

@Slf4j
@Component
@AllArgsConstructor
class Cml implements CommandLineRunner {

    private final FileNodeRepository repository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        User user = new User();
        user.setEmail("info@abc.com");
        user.setFirstName("Garry");
        user.setLastName("Johnson");
        user.setPasswordHash(passwordEncoder.encode("300819Nas"));
        user.setAuthType("password");
        User dbUser = userRepository.save(user);

        FileNode fileNode = new FileNode();
        fileNode.setName("GET THE GIRL!!! - The Office - 8x19 - Group Reaction.mp4");
        fileNode.setType("file");
        fileNode.setUserId(dbUser.getId());
        fileNode.setStoragePath("add13922-5710-48a4-aef1-a5ed3631de5b/GET THE GIRL!!! - The Office - 8x19 - Group Reaction.mp4");
        fileNode.setMimeType("video/mp4");
        fileNode.setSize(Long.parseLong("56405497"));
        fileNode.setHasThumbnail(false);
        FileNode dbfileNode = repository.save(fileNode);

        log.info("Saved file node: {}", dbfileNode);

        generateFileTree(user.getId(), 25, 24, null);
        var allFiles = repository.findAll();
        log.info("All file node: {}", allFiles.size());

    }

    private final AtomicInteger num = new AtomicInteger();
    private void generateFileTree(UUID userId, int size, int fileRatio, UUID parentId) {
        long minSize = 5L * 1024 * 1024; // 1MB
        long maxSize = 10L * 1024 * 1024; // 5MB

        ArrayList<UUID> folderIds = new ArrayList<>();
        int folder = size - fileRatio;

        for (int i = 1; i < size; i++) {
            int count = num.incrementAndGet();
            if (i <= folder) {
                FileNode fileNode = newFile("folder", "Folder " + count, null, parentId, userId);
                FileNode dbFileNode = repository.save(fileNode);
                folderIds.add(dbFileNode.getId());
            } else {
                var mimeType = MimeTypeUtil.getRandomMimeType();
                FileNode fileNode = newFile("file", "File " + count, mimeType, parentId, userId);
                long fileSize = minSize + (long)(random.nextDouble() * (maxSize - minSize));
                fileNode.setSize(fileSize);
                repository.save(fileNode);
            }
        }

        folderIds.forEach(folderId -> {
            generateFileTree(userId, size - 1,fileRatio + 1, folderId);
        });

    }

    private FileNode newFile(String type, String name, String mimeType, UUID parentId, UUID userId) {
        FileNode fileNode = new FileNode();
        fileNode.setUserId(userId);
        if (parentId != null) fileNode.setParentId(parentId);
        fileNode.setName(name);
        fileNode.setType(type);
        fileNode.setMimeType(mimeType);
        return fileNode;
    }

    public static void printFileNode(FileNode fileNode) {
        System.out.println("FileNode(id=" + fileNode.getId()
                + ", parentId=" + fileNode.getParentId() + ", name=" + fileNode.getName()
                + ", type=" + fileNode.getType() + ")");
    }
}