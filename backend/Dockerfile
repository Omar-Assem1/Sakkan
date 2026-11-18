# ==============================================================================
# Stage 1: Build the runnable Spring Boot JAR with Maven on Java 21
# ==============================================================================
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# 1) Copy only Maven wrapper and pom.xml first so dependencies are cached
#    in their own Docker layer unless pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml lombok.config ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# 2) Copy application source and build the fat JAR (tests run in CI)
COPY src/ src/
RUN ./mvnw -B -DskipTests package

# ==============================================================================
# Stage 2: Minimal Java 21 JRE runtime running as a non-root user
# ==============================================================================
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Create a dedicated non-root system user and group, plus the uploads directory
RUN groupadd --system sakan \
    && useradd --system --gid sakan --home-dir /app sakan \
    && mkdir -p /app/uploads \
    && chown -R sakan:sakan /app

COPY --from=build --chown=sakan:sakan /workspace/target/sakan-api-*.jar /app/app.jar

USER sakan
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
