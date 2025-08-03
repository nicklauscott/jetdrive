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
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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

        // doesn't work with H2 db
        //isBelowMaxFileCount(fileNode.getId()); // uncomment later

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

        //isBelowMaxFileCount(fileNode.getId()); // uncomment later; doesn't work with H2 db

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

@Slf4j
@Component
@AllArgsConstructor
class Cml implements CommandLineRunner {

    private final FileNodeRepository repository;
    private final FileChangeEventRepository eventRepository;
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
        user.setPicture("http://" + getHostAddress() + ":8001/public/profile-picture/info@abc_com_profile.jpeg");
        User dbUser = userRepository.save(user);

        FileNode fileNode = new FileNode();
        fileNode.setName("GET THE GIRL!!! - The Office - 8x19 - Group Reaction.mp4");
        fileNode.setType("file");
        fileNode.setUserId(dbUser.getId());
        fileNode.setObjectId("add13922-5710-48a4-aef1-a5ed3631de5b/GET THE GIRL!!! - The Office - 8x19 - Group Reaction.mp4");
        fileNode.setMimeType("video/mp4");
        fileNode.setSize(Long.parseLong("56405497"));
        fileNode.setHasThumbnail(false);
        hasEnoughSpace(user.getId(), 1024, fileNode.getSize());
        FileNode dbfileNode = repository.save(fileNode);

        log.info("Saved file node: {}", dbfileNode);

        generateFileTree(user.getId(), 5, 4, null);
    }

    private final AtomicInteger num = new AtomicInteger();
    private void generateFileTree(UUID userId, int size, int fileRatio, UUID parentId) {
        long minSize = 2L * 1024 * 1024;
        long maxSize = 3L * 1024 * 1024;

        ArrayList<UUID> folderIds = new ArrayList<>();
        int folder = size - fileRatio;

        for (int i = 1; i < size; i++) {
            int count = num.incrementAndGet();
            if (i <= folder) {
                FileNode fileNode = newFile("folder", "Folder " + count, null, parentId, userId);
                FileNode dbFileNode = repository.save(fileNode);
                saveEvent(fileNode, ChangeType.CREATED);
                folderIds.add(dbFileNode.getId());
            } else {
                var mimeType = MimeTypeUtil.getRandomMimeType();
                FileNode fileNode = newFile("file", "File " + count, mimeType, parentId, userId);
                long fileSize = minSize + (long)(random.nextDouble() * (maxSize - minSize));
                fileNode.setSize(fileSize);
                hasEnoughSpace(userId, 1024, fileSize);
                repository.save(fileNode);
                saveEvent(fileNode, ChangeType.CREATED);
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

    private void saveEvent(FileNode fileNode, ChangeType eventType) {
        FileChangeEvent event = new FileChangeEvent();
        event.setFileId(fileNode.getId());
        event.setParentId(fileNode.getParentId());
        event.setUserId(fileNode.getUserId());
        event.setEventType(eventType);
        event.setSnapShotJson(FileChangeEventMapper.toJson(FileNodeMapper.toDTO(fileNode)));
        eventRepository.save(event);
    }

    private void hasEnoughSpace(UUID userId, long totalSpaceMb, long fileSizeBytes) {
        long totalSpaceLong = totalSpaceMb * 1024 * 1034;
        long usedSpaceBytes = repository.getTotalStorageUsed(userId);
        if (totalSpaceLong - usedSpaceBytes < fileSizeBytes) {
            throw new FileNodeOperationException("You don't have enough space to perform this action");
        }
    }

    private String getHostAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                // Ignore down or loopback interfaces
                if (!iface.isUp() || iface.isLoopback()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            log.info("------------------------- Error: {}", e.getMessage());
            return  "";
        }
        return "";
    }
}