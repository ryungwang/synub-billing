package io.synub.billing.storage;

import io.synub.billing.config.AppProperties;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import jakarta.annotation.PreDestroy;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * 운영 파일 저장 — 사업자등록증 등을 PRIVATE S3 버킷에 저장. 자격증명은 기본 provider chain(인스턴스 롤/AWS_* env).
 * 키는 prefix로 앱별 네임스페이스 격리, 확장자만 유지. 접근은 billing이 load()로 바이트를 받아 스트리밍(공개 URL 없음).
 */
@Service
@Profile("prod")
public class S3StorageService implements StorageService {

    private final String bucket;
    private final String prefix;
    private final S3Client client;

    public S3StorageService(AppProperties props) {
        AppProperties.S3 s3 = props.storage() != null ? props.storage().s3() : null;
        if (s3 == null || !StringUtils.hasText(s3.bucket()) || !StringUtils.hasText(s3.region())) {
            throw new IllegalStateException("app.storage.s3.bucket, app.storage.s3.region 은 prod에서 필수입니다.");
        }
        this.bucket = s3.bucket();
        this.prefix = StringUtils.hasText(s3.prefix()) ? s3.prefix().replaceAll("/+$", "") : "synub-app";
        this.client = S3Client.builder()
                .region(Region.of(s3.region()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Override
    public String store(byte[] content, String originalFilename) {
        String ext = extension(originalFilename);
        String key = prefix + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
        // 클라이언트 Content-Type은 신뢰하지 않고 저장 키(확장자)로 안전 타입 추론(허용 확장자는 업로드 단계에서 이미 검증).
        client.putObject(PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType(safeContentType(ext)).build(),
                RequestBody.fromBytes(content));
        return key;
    }

    @Override
    public byte[] load(String key) {
        try {
            ResponseBytes<?> bytes = client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            return bytes.asByteArray();
        } catch (NoSuchKeyException e) {
            throw new NotFoundException("파일을 찾을 수 없습니다.");
        }
    }

    @PreDestroy
    void close() {
        client.close();
    }

    private static String safeContentType(String ext) {
        return switch (ext) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }

    private static String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return "";
        String ext = filename.substring(dot).toLowerCase();
        return ext.matches("\\.[a-z0-9]{1,8}") ? ext : "";
    }
}
