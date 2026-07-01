package io.synub.billing.storage;

/** 업로드 파일 저장 추상화. 로컬=파일시스템, 운영=S3(어댑터 교체). 특정 SDK 직접 의존 금지. */
public interface StorageService {

    /** 저장 후 키 반환. */
    String store(byte[] content, String originalFilename);

    /** 키로 원본 로드. */
    byte[] load(String key);
}
