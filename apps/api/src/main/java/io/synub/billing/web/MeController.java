package io.synub.billing.web;

import io.synub.billing.dto.Dtos.ProfileDto;
import io.synub.billing.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** 마이페이지 — 현재 통합계정 프로필(사진). 인증 필요(공개 아님). */
@RestController
@RequestMapping("/me")
public class MeController {

    private final ProfileService profiles;

    public MeController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/profile")
    public ProfileDto profile() {
        return new ProfileDto(profiles.avatarUrl());
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileDto uploadAvatar(@RequestPart("avatar") MultipartFile file) throws IOException {
        return new ProfileDto(profiles.uploadAvatar(file.getBytes(), file.getOriginalFilename()));
    }

    @DeleteMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvatar() {
        profiles.deleteAvatar();
    }
}
