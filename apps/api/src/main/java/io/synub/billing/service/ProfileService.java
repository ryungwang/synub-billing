package io.synub.billing.service;

import io.synub.billing.domain.UserProfile;
import io.synub.billing.repo.UserProfileRepository;
import io.synub.billing.storage.StorageService;
import io.synub.billing.web.ApiExceptions.BadRequestException;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 마이페이지 프로필(현재 통합계정) — 프로필 사진 업로드/삭제/조회. */
@Service
public class ProfileService {

    private static final int MAX_BYTES = 2 * 1024 * 1024; // 2MB

    private final UserProfileRepository profiles;
    private final StorageService storage;
    private final CurrentUser currentUser;

    public ProfileService(UserProfileRepository profiles, StorageService storage, CurrentUser currentUser) {
        this.profiles = profiles;
        this.storage = storage;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public String avatarUrl() {
        return profiles.findById(currentUser.externalId())
                .map(UserProfile::getAvatarKey)
                .filter(k -> k != null && !k.isBlank())
                .map(k -> "/avatars/" + k)
                .orElse(null);
    }

    @Transactional
    public String uploadAvatar(byte[] bytes, String originalFilename) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("파일이 비어 있습니다.");
        }
        if (bytes.length > MAX_BYTES) {
            throw new BadRequestException("이미지는 2MB 이하만 업로드할 수 있습니다.");
        }
        String ext = imageExt(bytes);
        if (ext == null) {
            throw new BadRequestException("이미지(JPG/PNG) 파일만 업로드할 수 있습니다.");
        }
        String storageKey = storage.store(bytes, "avatar" + ext);
        String avatarKey = UUID.randomUUID().toString().replace("-", "");
        String extId = currentUser.externalId();
        UserProfile p = profiles.findById(extId).orElseGet(() -> new UserProfile(extId));
        p.setAvatar(avatarKey, storageKey);
        profiles.save(p);
        return "/avatars/" + avatarKey;
    }

    @Transactional
    public void deleteAvatar() {
        profiles.findById(currentUser.externalId()).ifPresent(p -> {
            p.clearAvatar();
            profiles.save(p);
        });
    }

    @Transactional(readOnly = true)
    public Avatar loadAvatar(String avatarKey) {
        UserProfile p = profiles.findByAvatarKey(avatarKey)
                .filter(up -> up.getStorageKey() != null)
                .orElseThrow(() -> new NotFoundException("아바타를 찾을 수 없습니다."));
        return new Avatar(storage.load(p.getStorageKey()), contentType(p.getStorageKey()));
    }

    public record Avatar(byte[] content, String contentType) {}

    /** 매직바이트로 실제 이미지 종류 확인(확장자 위조 방지). PNG/JPEG만 허용. */
    private static String imageExt(byte[] b) {
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return ".png";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        return null;
    }

    private static String contentType(String key) {
        return key != null && key.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
    }
}
