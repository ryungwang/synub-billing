package io.synub.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 문의(contact form) 접수. 공개 문의 폼 제출을 저장한다.
 * 첨부 원본은 StorageService에 저장하고 여기엔 키·파일명·크기만 보관.
 */
@Entity
@Table(name = "inquiry")
public class Inquiry {

    public static final String OPEN = "open";
    public static final String RESOLVED = "resolved";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "product_label")
    private String productLabel;

    @Column(name = "name")
    private String name;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "attachment_key")
    private String attachmentKey;

    @Column(name = "attachment_filename")
    private String attachmentFilename;

    @Column(name = "attachment_size")
    private Integer attachmentSize;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Inquiry() {}

    public Inquiry(String type, String productCode, String productLabel, String name,
                   String email, String message, String externalId, String clientIp) {
        this.type = type;
        this.productCode = productCode;
        this.productLabel = productLabel;
        this.name = name;
        this.email = email;
        this.message = message;
        this.externalId = externalId;
        this.clientIp = clientIp;
        this.status = OPEN;
    }

    /** 첨부파일 저장 후 메타 기록. */
    public void attach(String key, String filename, int size) {
        this.attachmentKey = key;
        this.attachmentFilename = filename;
        this.attachmentSize = size;
    }

    /** 관리자 처리완료 표시. */
    public void resolve() {
        this.status = RESOLVED;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getProductCode() { return productCode; }
    public String getProductLabel() { return productLabel; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getMessage() { return message; }
    public String getAttachmentKey() { return attachmentKey; }
    public String getAttachmentFilename() { return attachmentFilename; }
    public Integer getAttachmentSize() { return attachmentSize; }
    public String getStatus() { return status; }
    public String getExternalId() { return externalId; }
    public String getClientIp() { return clientIp; }
    public Instant getCreatedAt() { return createdAt; }
}
