package io.synub.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** 통합계정(external_id)별 사용자 프로필 — 마이페이지 프로필 사진 등. */
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @Column(name = "external_id")
    private String externalId;

    /** 공개 서빙용 아바타 식별자(UUID). /avatars/{avatarKey} 로 노출. */
    @Column(name = "avatar_key")
    private String avatarKey;

    /** 내부 스토리지 키(S3/로컬). 외부 미노출. */
    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected UserProfile() {}

    public UserProfile(String externalId) {
        this.externalId = externalId;
    }

    public void setAvatar(String avatarKey, String storageKey) {
        this.avatarKey = avatarKey;
        this.storageKey = storageKey;
        this.updatedAt = Instant.now();
    }

    public void clearAvatar() {
        this.avatarKey = null;
        this.storageKey = null;
        this.updatedAt = Instant.now();
    }

    public String getExternalId() { return externalId; }
    public String getAvatarKey() { return avatarKey; }
    public String getStorageKey() { return storageKey; }
}
