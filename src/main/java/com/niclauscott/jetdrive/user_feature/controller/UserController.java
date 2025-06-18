package com.niclauscott.jetdrive.user_feature.controller;

import com.niclauscott.jetdrive.common.model.dtos.UpdateUserRequestDTO;
import com.niclauscott.jetdrive.common.model.dtos.UserResponseDTO;
import com.niclauscott.jetdrive.user_feature.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "User", description = "Api for managing users")
@AllArgsConstructor
public class UserController {

    private final UserService service;

    @GetMapping
    @Operation(description = "Get user")
    public ResponseEntity<UserResponseDTO> getUser() {
        return ResponseEntity.ok(service.getUser());
    }

    @PatchMapping
    @Operation(description = "Update user")
    public ResponseEntity<UserResponseDTO> updateUser(@Valid @RequestBody UpdateUserRequestDTO requestDTO) {
        return ResponseEntity.ok(service.updateUser(requestDTO));
    }

    @DeleteMapping
    @Operation(description = "Delete user")
    public ResponseEntity<Void> deleteUser() {
        service.deleteUser();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
