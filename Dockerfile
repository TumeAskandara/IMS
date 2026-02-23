# ============================================
# Stage 1: Build
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Cache dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# ============================================
# Stage 2: Runtime (hardened)
# ============================================
FROM eclipse-temurin:17-jre-alpine

# Security: create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Create directories the app needs
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# Copy artifact from build stage
COPY --from=build --chown=appuser:appgroup /build/target/*.jar app.jar

# Switch to non-root user
USER appuser:appgroup

EXPOSE 9443

# Health check via actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9443/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
