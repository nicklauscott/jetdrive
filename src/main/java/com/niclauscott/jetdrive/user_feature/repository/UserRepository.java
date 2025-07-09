package com.niclauscott.jetdrive.user_feature.repository;

import com.niclauscott.jetdrive.user_feature.model.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> deleteByEmail(String email);

    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END 
        FROM User u 
        WHERE u.id = :userId 
        AND u.isEnabled = true 
        AND u.usedSpaceMb + :fileSizeMb <= u.quotaLimitMb
    """)
    boolean canUserUpload(@Param("userId") UUID userId, @Param("fileSizeMb") int fileSizeMb);

}
