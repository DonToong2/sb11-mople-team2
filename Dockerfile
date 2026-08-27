# --- 1단계 : 빌드 ---
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# 의존성 캐시를 위해 gradle 관련 파일 먼저 복사
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

# 소스 복사 후 빌드 (테스트는 CI에서 검증되므로 스킵)
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# --- 2단계 : 실행 ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# non-root 사용자로 실행
RUN useradd -r -u 1001 appuser

# 로그 디렉터리 + 인증서 디렉터리 생성 및 appuser 권한 설정
RUN mkdir -p /app/.logs /app/certs \
    && chown -R appuser:appuser /app

COPY --from=build /app/build/libs/mople-0.0.1-SNAPSHOT.jar app.jar
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh && chown appuser:appuser /app/entrypoint.sh

USER appuser

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod

# 기존 값들 entrypoint.sh로 이동
ENTRYPOINT ["/app/entrypoint.sh"]