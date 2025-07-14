package com.niclauscott.jetdrive.file_feature.file.service;

import com.niclauscott.jetdrive.common.model.UserPrincipal;
import com.niclauscott.jetdrive.file_feature.file.model.dtos.FileChangeEventDTO;
import com.niclauscott.jetdrive.file_feature.file.model.mapper.FileChangeEventMapper;
import com.niclauscott.jetdrive.file_feature.file.repository.FileChangeEventRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class SyncService {

    private final FileChangeEventRepository repository;

    public List<FileChangeEventDTO> getChangeSince(LocalDateTime since) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        return repository.findAllByUserIdAndTimeStampAfterOrderByTimeStampAsc(userPrincipal.getUserId(), since)
                .stream().map(FileChangeEventMapper::toDTO).toList();
    }

}
