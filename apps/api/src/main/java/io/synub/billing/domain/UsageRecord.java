package io.synub.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** 제품이 보고한 이번 청구주기 사용량. (external_id, service_code) 유니크. */
@Entity
@Table(name = "usage_record")
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "service_code", nullable = false)
    private String serviceCode;

    private String label;
    private String unit;
    private int used;

    @Column(name = "limit_qty")
    private Integer limitQty;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected UsageRecord() {}

    public UsageRecord(String externalId, String serviceCode) {
        this.externalId = externalId;
        this.serviceCode = serviceCode;
    }

    public void report(String label, String unit, int used, Integer limitQty) {
        this.label = label;
        this.unit = unit;
        this.used = used;
        this.limitQty = limitQty;
        this.updatedAt = Instant.now();
    }

    public String getLabel() { return label; }
    public String getUnit() { return unit; }
    public int getUsed() { return used; }
    public Integer getLimitQty() { return limitQty; }
}
