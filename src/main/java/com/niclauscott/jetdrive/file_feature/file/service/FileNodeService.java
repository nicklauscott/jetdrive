package com.niclauscott.jetdrive.file_feature.file.service;

import com.niclauscott.jetdrive.common.model.UserPrincipal;
import com.niclauscott.jetdrive.file_feature.common.exception.FileNotFoundException;
import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileNodeDTO;
import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileNodeTreeResponse;
import com.niclauscott.jetdrive.file_feature.file.model.entities.FileNode;
import com.niclauscott.jetdrive.file_feature.file.model.mapper.FileNodeMapper;
import com.niclauscott.jetdrive.file_feature.file.repository.FileNodeRepository;
import com.niclauscott.jetdrive.user_feature.model.entities.User;
import com.niclauscott.jetdrive.user_feature.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class FileNodeService {

    private final FileNodeRepository repository;

    public Optional<FileNodeTreeResponse> getFiles() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

        List<FileNode> rootFiles = repository.findByUserIdAndParentIdIsNull(userPrincipal.getUserId());
        List<FileNodeDTO> dtoList = rootFiles.stream().map(FileNodeMapper::toDTO).toList();

        return Optional.of(new FileNodeTreeResponse(null, null, dtoList));
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

    public FileNodeDTO createFileNode(
            String name, String type, String parentId, long size,
            String storagePath,
            boolean hasThumbnail, String thumbnailPath
    ) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = new FileNode();
        fileNode.setUserId(userPrincipal.getUserId());
        if (parentId != null) fileNode.setParentId(UUID.fromString(parentId));
        fileNode.setName(name);
        fileNode.setType(type);
        fileNode.setMimeType(MimeTypeUtil.getMimeTypeByExtension(name));
        fileNode.setSize(size);
        fileNode.setStoragePath(storagePath);
        fileNode.setHasThumbnail(hasThumbnail);
        if (thumbnailPath != null) fileNode.setThumbnailPath(thumbnailPath);
        return FileNodeMapper.toDTO(repository.save(fileNode));
    }

    @Transactional
    public void renameFileNode(String id, String name) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), UUID.fromString(id))

                .orElseThrow(() -> new FileNotFoundException("file with the id not found"));
        fileNode.setName(name);
        repository.save(fileNode);
    }

    @Transactional
    public void deleteFileNode(UUID id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        repository.deleteByUserIdAndId(userPrincipal.getUserId(), id);
        // Delete thumbnail and the actual file
    }

    @Transactional
    public void copyFileNode(String id, String parentID) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), UUID.fromString(id))
                .orElseThrow(() -> new FileNotFoundException("file with the id not found"));
        fileNode.setId(UUID.randomUUID()); // create a copy of the old file node
        fileNode.setParentId(UUID.fromString(parentID));
        repository.save(fileNode);
    }

    @Transactional
    public void moveFileNode(String id, String newParentID) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        FileNode fileNode = repository.findByUserIdAndId(userPrincipal.getUserId(), UUID.fromString(id))
                .orElseThrow(() -> new FileNotFoundException("file with the id not found"));
        fileNode.setParentId(UUID.fromString(newParentID)); // move the file node to a new parent (i.e., folder)
        repository.save(fileNode);
    }
}

@Slf4j
@Component
@AllArgsConstructor
class Cml implements CommandLineRunner {
    private final FileNodeRepository repository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(50);

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        User user = new User();
        user.setEmail("info@abc.com");
        user.setPasswordHash(passwordEncoder.encode("pass12345"));
        user.setAuthType("password");
        userRepository.save(user);

        //loadFileNodes(user.getId());

    }

    private void loadFileNodes(UUID userId) {
        List<FileNode> fileNodes = List.of(1,2,3,4,5).stream().map(i ->
                newFile("folder", "folder " + i, null, userId)).toList();
        fileNodes.forEach(repository::save);

        for (FileNode fileNode: repository.findAll()) {
            for (int i = 0; i < random.nextInt(25); i++) {
                String typeAndName = (i + random.nextInt() % 2 == 0) ? "folder" : "file";
                FileNode file = newFile(typeAndName, typeAndName + i, fileNode.getId(), userId);
                repository.save(file);
                if (typeAndName.equals("folder")) {
                    for (int j = 0; j < random.nextInt(5); j++) {
                        FileNode fil = newFile("file", typeAndName + i + j, file.getId(), userId);
                        repository.save(fil);
                    }
                }
            }
        }

        log.info("---------------------------------- Files start");
        repository.findAll().forEach(Cml::printFileNode);
        log.info("---------------------------------- Files End");
    }

    private FileNode newFile(String type, String name, UUID parentId, UUID userId) {
        FileNode fileNode = new FileNode();
        fileNode.setUserId(userId);
        if (parentId != null) fileNode.setParentId(parentId);
        fileNode.setName(name);
        fileNode.setType(type);
        return fileNode;
    }

    public static void printFileNode(FileNode fileNode) {
        System.out.println("FileNode(id=" + fileNode.getId()
                + ", parentId=" + fileNode.getParentId() + ", name=" + fileNode.getName()
                + ", type=" + fileNode.getType() + ")");
    }
}