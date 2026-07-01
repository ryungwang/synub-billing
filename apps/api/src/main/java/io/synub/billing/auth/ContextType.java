package io.synub.billing.auth;

/** 현재 요청이 어떤 컨텍스트로 행동하는가. 개인(내 카드/구독) vs 조직(법인카드/회사구독). */
public enum ContextType {
    PERSONAL,
    ORG
}
