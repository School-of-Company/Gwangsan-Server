# === Build Stage ===
# 테스트는 CI/CD 워크플로에서 이미지 빌드 전에 `./gradlew test`로 실행한다.
FROM eclipse-temurin:21.0.12.1_1-jdk-alpine-3.24@sha256:fd52084c35bbac78890d95d57707d31ffc9d7a3324199e89c06be2a9dc71bfa0 AS build

WORKDIR /app

COPY gradle ./gradle
COPY build.gradle settings.gradle gradlew ./
COPY src ./src

RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test --no-daemon

# OpenTelemetry Java agent: Maven Central 2.33.1, GitHub release asset digest와 동일한 SHA-256으로 검증
ARG OTEL_JAVAAGENT_VERSION=2.33.1
ARG OTEL_JAVAAGENT_SHA256=e43a270df53c6f423e0ab157be9c27ccc01d5dc3232e162fe54777573db2f149
RUN wget -q -O /app/opentelemetry-javaagent.jar \
      "https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/${OTEL_JAVAAGENT_VERSION}/opentelemetry-javaagent-${OTEL_JAVAAGENT_VERSION}.jar" \
 && echo "${OTEL_JAVAAGENT_SHA256}  /app/opentelemetry-javaagent.jar" | sha256sum -c -

# === Run Stage ===
FROM eclipse-temurin:21.0.12.1_1-jre-alpine-3.24@sha256:b48e02e0ea88d7bfb305335df94923bdef394905f4147dccd52e873cf17b1088

WORKDIR /app

COPY --from=build /app/build/libs/*SNAPSHOT.jar app.jar
COPY --from=build /app/opentelemetry-javaagent.jar opentelemetry-javaagent.jar

# 트레이스만 OTLP HTTP로 Alloy에 보낸다. 메트릭은 Actuator(9091), 로그는 Promtail 담당.
# 롤백: 런타임에 OTEL_JAVAAGENT_ENABLED=false 를 주면 에이전트가 계측 없이 비활성화된다.
# 개인정보: Lettuce 자동 span 은 끈다. JWT 가 Redis 키라 db.statement(EXISTS <JWT>)로 원문이 노출됐다(E2E 확인).
#   HTTP/JDBC/@Async 와 Streams CONSUMER 커스텀 span 은 유지. 명령 이름만 남기는 안전한 캡처가 생기면 재검토.
ENV OTEL_SERVICE_NAME=gwangsan-api \
    OTEL_RESOURCE_ATTRIBUTES=deployment.environment.name=development \
    OTEL_TRACES_EXPORTER=otlp \
    OTEL_METRICS_EXPORTER=none \
    OTEL_LOGS_EXPORTER=none \
    OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf \
    OTEL_EXPORTER_OTLP_ENDPOINT=http://alloy:4318 \
    OTEL_PROPAGATORS=tracecontext,baggage \
    OTEL_TRACES_SAMPLER=parentbased_traceidratio \
    OTEL_TRACES_SAMPLER_ARG=1.0 \
    OTEL_INSTRUMENTATION_LETTUCE_ENABLED=false

# 9091(management)은 컨테이너 내부 전용이며 EXPOSE/publish 하지 않는다.
EXPOSE 8080

ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]
