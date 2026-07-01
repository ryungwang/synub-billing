package io.synub.billing.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** 멱등키 레코드. (scope, idemKey) 유니크. 완료되면 응답(JSON)을 저장해 재요청 시 재사용. */
@Entity
@Table(name = "idempotency_key")
public class IdempotencyKey {

    public static final String IN_PROGRESS = "in_progress";
    public static final String COMPLETED = "completed";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String scope;

    @Column(name = "idem_key", nullable = false)
    private String idemKey;

    private String customer;

    private String status;

    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyKey() {}

    public IdempotencyKey(String scope, String idemKey, String customer) {
        this.scope = scope;
        this.idemKey = idemKey;
        this.customer = customer;
        this.status = IN_PROGRESS;
    }

    public void complete(String responseBody) {
        this.status = COMPLETED;
        this.responseBody = responseBody;
    }

    public boolean isCompleted() { return COMPLETED.equals(status); }

    public Long getId() { return id; }
    public String getScope() { return scope; }
    public String getIdemKey() { return idemKey; }
    public String getCustomer() { return customer; }
    public String getStatus() { return status; }
    public String getResponseBody() { return responseBody; }
}
