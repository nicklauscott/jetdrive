package com.niclauscott.jetdrive.user_feature.service;

import com.niclauscott.jetdrive.common.model.UserPrincipal;
import com.niclauscott.jetdrive.auth_feature.model.dtos.CreateUserRequestDTO;
import com.niclauscott.jetdrive.common.model.dtos.UpdateUserRequestDTO;
import com.niclauscott.jetdrive.common.model.dtos.UserResponseDTO;
import com.niclauscott.jetdrive.file_feature.common.exception.CantUploadFileException;
import com.niclauscott.jetdrive.file_feature.file.service.FileNodeService;
import com.niclauscott.jetdrive.file_feature.upload.service.UploadService;
import com.niclauscott.jetdrive.user_feature.exception.UserAlreadyExistException;
import com.niclauscott.jetdrive.user_feature.exception.UserDoesntExistException;
import com.niclauscott.jetdrive.user_feature.model.entities.User;
import com.niclauscott.jetdrive.user_feature.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final FileNodeService fileNodeService;
    private final UploadService uploadService;

    public void upload(MultipartFile file) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        User user = repository.findByEmail(userPrincipal.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        try {
            String address = getHostAddress();
            String oldImagePath = null;
            if (Objects.equals(user.getAuthType(), "password")) {
               oldImagePath = user.getPicture() != null ? user.getPicture().split("picture/")[1] : null;
            }
            String imagePath = uploadService.uploadProfilePicture(file, user.getEmail(), oldImagePath);
            user.setPicture("http://" + address + ":9000/" + imagePath);
            repository.save(user);
        } catch (IOException e) {
            throw new CantUploadFileException("Error occurred when uploading picture");
        }
    }

    public Optional<User> getUserByEmail(String email) {
        return repository.findByEmail(email);
    }

    public UserResponseDTO getUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        User user = repository.findByEmail(userPrincipal.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserResponseDTO(user.getEmail(), user.getFirstName(), user.getLastName(), user.getPicture());
    }

    public User createUser(User user) {
        if (repository.findByEmail(user.getEmail()).isPresent()) return user;
        User dbUser = repository.save(user);
        fileNodeService.createFileNodeForNewUser(dbUser.getId());
        return dbUser;
    }

    public UserResponseDTO createUser(CreateUserRequestDTO requestDTO, String authType) {
        if (repository.findByEmail(requestDTO.getEmail()).isPresent())
            throw new UserAlreadyExistException("A User with the email " + requestDTO.getEmail() + " already exist");

        User user = new User();
        user.setEmail(requestDTO.getEmail());
        user.setFirstName(requestDTO.getFirstName());
        user.setLastName(requestDTO.getLastName());
        user.setPasswordHash(requestDTO.getPassword());
        user.setAuthType(authType);
        user.setPicture(requestDTO.getPicUrl());
        User savedUser = repository.save(user);
        fileNodeService.createFileNodeForNewUser(savedUser.getId());
        return new UserResponseDTO(savedUser.getEmail(), savedUser.getFirstName(), savedUser.getLastName(), savedUser.getPicture());
    }

    @Transactional
    public UserResponseDTO updateUser(UpdateUserRequestDTO requestDTO) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        Optional<User> optional = repository.findByEmail(userPrincipal.getUsername());
        if (optional.isEmpty()) throw new UserDoesntExistException("User with the provided email doesn't exist");

        User user = optional.get();
        if (requestDTO.getEmail() != null && !requestDTO.getEmail().isBlank())
            user.setEmail(requestDTO.getEmail());
        if (requestDTO.getFirstName() != null && !requestDTO.getFirstName().isBlank())
            user.setFirstName(requestDTO.getFirstName());
        if (requestDTO.getLastName() != null && !requestDTO.getLastName().isBlank())
            user.setLastName(requestDTO.getLastName());
        User savedUser = repository.save(user);
        return new UserResponseDTO(savedUser.getEmail(), savedUser.getFirstName(), savedUser.getLastName(), savedUser.getPicture());
    }

    public void deleteUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        fileNodeService.deleteUserFiles(userPrincipal.getUserId());
        repository.deleteByEmail(userPrincipal.getUsername());
    }

    // -------------------------------------- Remove later -----------------------------------
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

