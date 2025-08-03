package com.niclauscott.jetdrive.user_feature.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "avatar_uri")
    private String picture;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "auth_type", nullable = false)
    private String authType;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = true;

    @Column(name = "quota_limit_mb", nullable = false)
    private Integer quotaLimitMb = 100;

    @Column(name = "used_space_mb", nullable = false)
    private Integer usedSpaceMb = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}
