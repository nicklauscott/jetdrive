package com.niclauscott.jetdrive.auth_feature.model.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequestDTO {
    @Email(message = "A valid email address is required")
    private String email;

    @NotNull(message = "First name is required")
    @Size(min = 3, message = "First name must be at least 3 characters long")
    private String firstName;

    @NotNull(message = "Last name is required")
    @Size(min = 3, message = "Last name must be at least 3 characters long")
    private String lastName;

    @NotNull(message = "Password is required")
    @Size(min = 6, message = "Password name must be at least 6 characters long")
    private String password;

    private String picUrl;
}
