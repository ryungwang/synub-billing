package io.synub.billing.service;

/** 빌링 소유 스코프 — 개인(customer) 또는 조직(organization). 구독·카드·조회의 소유 단위. */
public record Owner(String type, Long id) {

    public static final String CUSTOMER = "customer";
    public static final String ORGANIZATION = "organization";

    public static Owner customer(Long id) { return new Owner(CUSTOMER, id); }
    public static Owner organization(Long id) { return new Owner(ORGANIZATION, id); }

    public boolean isOrganization() { return ORGANIZATION.equals(type); }
}
