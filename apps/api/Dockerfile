# syntax=docker/dockerfile:1
#
# synub-billing API image. 루트 Gradle 멀티프로젝트(:apps:api) — 빌드 컨텍스트 = repo 루트.
#   docker build -f apps/api/Dockerfile -t synub-billing-api .
# (CI sets context: . and file: apps/api/Dockerfile.)
#
# 런타임 env 계약(compose가 주입): SPRING_PROFILES_ACTIVE=prod,
#   SPRING_DATASOURCE_USERNAME/PASSWORD(필수), SPRING_DATASOURCE_URL(선택 — 기본 db:5432/synub_billing),
#   WEBHOOK_SECRET, SERVICE_API_KEY, INTERNAL_SECRET, PORTONE_STORE_ID/CHANNEL_KEY/API_SECRET/WEBHOOK_SECRET,
#   SSO_ISSUER/SSO_JWKS_URI, APP_CORS_ALLOWED_ORIGINS, (선택)MAIL_*·BUSINESS_API_KEY. 시크릿은 이미지에 안 넣는다.

# --- build stage: Gradle bootJar (JDK 21) ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 루트 래퍼/설정 + 서브프로젝트 빌드파일 먼저 → 의존성 레이어 캐싱.
COPY gradlew settings.gradle.kts ./
COPY gradle ./gradle
COPY apps/api/build.gradle.kts apps/api/build.gradle.kts
RUN chmod +x gradlew && ./gradlew --no-daemon :apps:api:dependencies > /dev/null 2>&1 || true

# 소스 후 boot jar 빌드(테스트 제외).
COPY apps/api/src apps/api/src
RUN ./gradlew --no-daemon :apps:api:clean :apps:api:bootJar -x test

# --- run stage: JRE 21 runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -g 10001 app && useradd -u 10001 -g app -r app
COPY --from=build /app/apps/api/build/libs/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
