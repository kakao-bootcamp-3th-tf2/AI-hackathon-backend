# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 의존성 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Gradle wrapper 실행 권한 부여
RUN chmod +x gradlew

# 의존성 다운로드
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 빌드 (테스트 스킵) - 필요 시: -x test 추가
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# JVM 최적화 옵션 (t4g.small: 2 vCPU, 2GB RAM)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:+UseStringDeduplication", \
  "-XX:ActiveProcessorCount=2", \
  "-Xss512k", \
  "-jar", "app.jar"]

