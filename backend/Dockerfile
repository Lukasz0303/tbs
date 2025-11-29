ARG JAVA_VERSION=21
ARG JAVA_RUNTIME_VERSION=21-jre
ARG GRADLE_VERSION=8.10.2

FROM gradle:${GRADLE_VERSION}-jdk${JAVA_VERSION} AS builder

LABEL maintainer="tbswars"
LABEL description="Backend build stage for World at War: Turn-Based Strategy"

WORKDIR /app

COPY gradle gradle
COPY build.gradle settings.gradle gradlew ./
COPY gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.jar

RUN chmod +x ./gradlew && \
    ./gradlew dependencies --no-daemon || true

COPY src src

ARG BUILD_VERSION=latest
ENV BUILD_VERSION=${BUILD_VERSION}

RUN ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:${JAVA_RUNTIME_VERSION} AS runtime

LABEL maintainer="tbswars"
LABEL description="Backend runtime stage for World at War: Turn-Based Strategy"

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd -r spring && \
    useradd -r -g spring spring && \
    mkdir -p /app && \
    chown -R spring:spring /app

WORKDIR /app

COPY --from=builder --chown=spring:spring /app/build/libs/tbs-*.jar app.jar

USER spring

EXPOSE 4333

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=4333
ENV SERVER_ADDRESS=0.0.0.0
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:4333/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

