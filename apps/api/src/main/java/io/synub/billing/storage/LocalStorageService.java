package io.synub.billing.storage;

import io.synub.billing.config.AppProperties;
import io.synub.billing.web.ApiExceptions.BadRequestException;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/** 로컬 파일시스템 저장(dev). app.storage.dir 하위에 보관. 운영은 S3StorageService(@Profile prod). */
@Service
@Profile("!prod")
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(AppProperties props) {
        this.root = Paths.get(props.storage() != null && props.storage().dir() != null
                ? props.storage().dir() : "./data/uploads").toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("스토리지 초기화 실패: " + root, e);
        }
    }

    @Override
    public String store(byte[] content, String originalFilename) {
        String ext = extension(originalFilename);
        String key = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path target = resolve(key);
            Files.write(target, content);
            return key;
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 실패", e);
        }
    }

    @Override
    public byte[] load(String key) {
        Path target = resolve(key);
        if (!Files.exists(target)) {
            throw new NotFoundException("파일을 찾을 수 없습니다.");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new IllegalStateException("파일 로드 실패", e);
        }
    }

    /** 경로 이탈(../) 방지 — 저장 루트 밖으로 나가지 않도록 검증. */
    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new BadRequestException("잘못된 파일 키입니다.");
        }
        return p;
    }

    private String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return "";
        String ext = filename.substring(dot).toLowerCase();
        return ext.matches("\\.[a-z0-9]{1,8}") ? ext : "";
    }
}
