# ---- Build Stage ----
FROM gradle:9.1-jdk21-alpine AS build
WORKDIR /app

# copy files for dependency resolution
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon

# Copy everything and build the fat jar
COPY . .
RUN gradle clean buildFatJar --no-daemon

# ---- Run Stage ----
FROM eclipse-temurin:21-jre-jammy AS run
WORKDIR /app

# Copy only the fat jar from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 5000

# Define healthchecks
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=10s \
    CMD curl -f http://localhost:5000/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
