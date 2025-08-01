package com.niclauscott.jetdrive.user_feature.controller;

import com.niclauscott.jetdrive.common.model.dtos.UpdateUserRequestDTO;
import com.niclauscott.jetdrive.common.model.dtos.UserResponseDTO;
import com.niclauscott.jetdrive.user_feature.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
@Tag(name = "User", description = "Api for managing users")
@AllArgsConstructor
@Slf4j
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
        return new ResponseEntity<>(service.updateUser(requestDTO), HttpStatus.ACCEPTED);
    }

    @PostMapping
    @Operation(description = "Upload profile picture")
    ResponseEntity<?> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
        service.upload(file);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @DeleteMapping
    @Operation(description = "Delete user")
    public ResponseEntity<Void> deleteUser() {
        service.deleteUser();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
