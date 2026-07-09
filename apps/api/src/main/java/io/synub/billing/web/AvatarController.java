package io.synub.billing.web;

import io.synub.billing.service.ProfileService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** 아바타 공개 서빙 — 등록된 avatar_key(UUID)만. 임의 스토리지 키 접근 불가. IdentityFilter 공개 경로. */
@RestController
@RequestMapping("/avatars")
public class AvatarController {

    private final ProfileService profiles;

    public AvatarController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/{key}")
    public ResponseEntity<byte[]> avatar(@PathVariable String key) {
        ProfileService.Avatar a = profiles.loadAvatar(key);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(a.content());
    }
}
