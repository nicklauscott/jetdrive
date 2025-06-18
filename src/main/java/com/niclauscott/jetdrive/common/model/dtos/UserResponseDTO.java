package com.niclauscott.jetdrive.common.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponseDTO {
    private String email;
    private String firstName;
    private String lastName;
}
