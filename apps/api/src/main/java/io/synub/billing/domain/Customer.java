package io.synub.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    private String email;

    private String phone;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Customer() {}

    public Customer(String externalId, String email) {
        this.externalId = externalId;
        this.email = email;
    }

    public void setPhone(String phone) { this.phone = phone; }

    /** PG 청구에 쓸 전화번호. 미수집이면 안전한 기본값(Mock/일부 채널 허용). */
    public String phoneForBilling() {
        return phone == null || phone.isBlank() ? "010-0000-0000" : phone;
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Instant getCreatedAt() { return createdAt; }
}
