package com.niclauscott.jetdrive.auth_feature.model.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequestDTO {
    @Email(message = "A valid email address is required")
    private String email;
    @NotNull(message = "Password is required")
    private String password;
}
