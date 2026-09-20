# syntax=docker/dockerfile:1
# Uses the jar built locally by `mvn clean package -DskipTests`.
# Rebuild the jar after code changes before running docker compose up --build.

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S invoiceguard && adduser -S invoiceguard -G invoiceguard

WORKDIR /app
COPY target/invoiceguard-api.jar app.jar

RUN mkdir -p /app/data/documents && chown -R invoiceguard:invoiceguard /app

USER invoiceguard

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]