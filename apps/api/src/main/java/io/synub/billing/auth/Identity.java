package io.synub.billing.auth;

/**
 * 검증된 현재 신원. externalId = 통합계정(SSO) 사용자 식별자(customer.external_id 와 대응).
 * email 은 토큰 claim 에서 옴(신규 고객 provisioning 시 사용). context 는 개인/조직 모드.
 */
public record Identity(String externalId, String email, AuthContext context, boolean admin) {

    public Identity withContext(AuthContext newContext) {
        return new Identity(externalId, email, newContext, admin);
    }
}
