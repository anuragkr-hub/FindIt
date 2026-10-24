# ==============================================================================
# Stage 1: Build Application with Maven & Temurin JDK 21
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy frontend source templates and static assets
COPY frontend/ /build/frontend/

# Copy backend pom and sources
COPY backend/pom.xml /build/backend/
COPY backend/src /build/backend/src

# Build production jar
WORKDIR /build/backend
RUN mvn clean package -DskipTests

# ==============================================================================
# Stage 2: Minimal Production JRE Runtime
# ==============================================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create unprivileged user for security
RUN groupadd -r findit && useradd -r -g findit findit

# Create uploads directory and grant permissions
RUN mkdir -p /app/uploads && chown -R findit:findit /app

# Copy executable jar from builder stage
COPY --from=builder /build/backend/target/findit-backend-*.jar /app/app.jar

USER findit:findit

EXPOSE 8080

ENV SERVER_PORT=8080 \
    APP_UPLOAD_DIR=/app/uploads

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
