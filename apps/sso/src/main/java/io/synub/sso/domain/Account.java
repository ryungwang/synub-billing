package io.synub.sso.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** 통합계정. 인증 정보(이메일·비번해시·external_id)만 보관. 결제·구독은 빌링 소관. */
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, updatable = false)
    private String externalId;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String name;

    private String status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Account() {}

    public Account(String externalId, String email, String passwordHash, String name) {
        this.externalId = externalId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.status = "active";
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
