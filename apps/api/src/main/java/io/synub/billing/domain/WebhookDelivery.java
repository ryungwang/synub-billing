package io.synub.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "webhook_delivery")
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    private String event;
    private String url;

    @Column(columnDefinition = "text")
    private String payload;

    private String status;
    private int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected WebhookDelivery() {}

    public WebhookDelivery(Long productId, String event, String url, String payload) {
        this.productId = productId;
        this.event = event;
        this.url = url;
        this.payload = payload;
        this.status = "pending";
    }

    public Long getId() { return id; }
    public String getEvent() { return event; }
    public String getUrl() { return url; }
    public String getPayload() { return payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
}
