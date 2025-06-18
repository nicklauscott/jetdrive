package com.niclauscott.jetdrive.user_feature.repository;

import com.niclauscott.jetdrive.user_feature.model.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> deleteByEmail(String email);
}
