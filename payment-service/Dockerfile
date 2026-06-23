# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper and dependency files first for layer caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon 2>/dev/null || true

# Copy source and build the jar, skipping tests (tests run in CI)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the built jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
