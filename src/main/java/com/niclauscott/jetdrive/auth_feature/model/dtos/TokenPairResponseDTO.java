package com.niclauscott.jetdrive.auth_feature.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenPairResponseDTO {
    private String access;
    private String refresh;
}
