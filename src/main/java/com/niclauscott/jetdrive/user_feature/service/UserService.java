package com.niclauscott.jetdrive.user_feature.service;

import com.niclauscott.jetdrive.common.model.UserPrincipal;
import com.niclauscott.jetdrive.auth_feature.model.dtos.CreateUserRequestDTO;
import com.niclauscott.jetdrive.common.model.dtos.UpdateUserRequestDTO;
import com.niclauscott.jetdrive.common.model.dtos.UserResponseDTO;
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
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository repository;

    public Optional<User> getUserByEmail(String email) {
        return repository.findByEmail(email);
    }

    public UserResponseDTO getUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        User user = repository.findByEmail(userPrincipal.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserResponseDTO(user.getEmail(), user.getFirstName(), user.getLastName());
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
        User savedUser = repository.save(user);
        return new UserResponseDTO(savedUser.getEmail(), savedUser.getFirstName(), savedUser.getLastName());
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
        return new UserResponseDTO(savedUser.getEmail(), savedUser.getFirstName(), savedUser.getLastName());
    }

    public void deleteUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        repository.deleteByEmail(userPrincipal.getUsername());
    }
}

