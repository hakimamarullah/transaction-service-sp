FROM ghcr.io/graalvm/graalvm-community:21 AS builder


WORKDIR /opt/app

# Copy Maven wrapper and pom.xml (relative to project root)
COPY .mvn/ .mvn/
COPY mvnw pom.xml lombok.config ./

RUN chmod +x ./mvnw

# Copy source code
COPY src/ ./src/
RUN ./mvnw clean install -Pnative -DskipTests -Dspring.profiles.active=test


FROM debian:bookworm-slim

# Create non-root user
RUN groupadd -r spring && useradd -r -g spring spring

ENV LOG_DIR=/opt/app/logs
ENV KAFKA_STATE_DIR=/opt/app/kafka_state

RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    ca-certificates \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Copy jar with fixed name
COPY --from=builder --chown=spring:spring /opt/app/target/transactions-service /opt/app/transactions-service

RUN mkdir -p "$LOG_DIR" && chown -R spring:spring "$LOG_DIR" && \
    mkdir -p "$KAFKA_STATE_DIR" && chown -R spring:spring "$KAFKA_STATE_DIR"

# Switch to non-root user
USER spring

EXPOSE 8080

# Set default values for environment variables
ENV JAVA_OPTS="" \
    JAVA_ARGS=""

# Use exec form with sh to handle environment variables properly
ENTRYPOINT ["sh", "-c", "exec /opt/app/transactions-service $JAVA_OPTS $JAVA_ARGS"]