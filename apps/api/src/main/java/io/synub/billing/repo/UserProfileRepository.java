package io.synub.billing.repo;

import io.synub.billing.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByAvatarKey(String avatarKey);
}
