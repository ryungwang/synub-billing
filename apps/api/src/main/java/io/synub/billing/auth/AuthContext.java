package io.synub.billing.auth;

/**
 * 현재 요청의 행동 컨텍스트. personal = 개인, org:{id} = 특정 조직.
 * HTTP 헤더 {@code X-Synub-Context} 로 전달되며, 값이 없으면 개인(personal)으로 간주한다.
 */
public record AuthContext(ContextType type, Long orgId) {

    public static AuthContext personal() {
        return new AuthContext(ContextType.PERSONAL, null);
    }

    public static AuthContext org(long orgId) {
        return new AuthContext(ContextType.ORG, orgId);
    }

    public boolean isPersonal() {
        return type == ContextType.PERSONAL;
    }

    public boolean isOrg() {
        return type == ContextType.ORG;
    }

    /**
     * X-Synub-Context 헤더 파싱. "personal"/빈값 → 개인, "org:{id}" → 조직.
     * 형식이 이상하면 fail-closed 로 예외를 던진다(임의로 개인 처리하지 않음).
     */
    public static AuthContext parse(String header) {
        if (header == null || header.isBlank() || header.equalsIgnoreCase("personal")) {
            return personal();
        }
        String h = header.trim();
        if (h.regionMatches(true, 0, "org:", 0, 4)) {
            try {
                return org(Long.parseLong(h.substring(4).trim()));
            } catch (NumberFormatException e) {
                throw new AuthException("잘못된 조직 컨텍스트: " + header);
            }
        }
        throw new AuthException("알 수 없는 컨텍스트: " + header);
    }
}
