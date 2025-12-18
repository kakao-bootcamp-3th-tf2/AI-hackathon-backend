# Runtime 이미지
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 빌드된 JAR 복사 (CI에서 ./gradlew bootJar 실행 필요)
COPY build/libs/*.jar app.jar

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

