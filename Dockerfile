# syntax=docker/dockerfile:1
# Build inside Docker so a Git-based host does not need a prebuilt target/*.jar.
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S invoiceguard && adduser -S invoiceguard -G invoiceguard

WORKDIR /app
COPY --from=build /build/target/invoiceguard-api.jar app.jar

RUN mkdir -p /app/data/documents && chown -R invoiceguard:invoiceguard /app

USER invoiceguard

EXPOSE 8080


HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
