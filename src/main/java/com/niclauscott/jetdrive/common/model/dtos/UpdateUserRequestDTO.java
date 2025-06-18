package com.niclauscott.jetdrive.common.model.dtos;

import lombok.Data;

@Data
public class UpdateUserRequestDTO {
    private String email;
    private String firstName;
    private String lastName;
}
