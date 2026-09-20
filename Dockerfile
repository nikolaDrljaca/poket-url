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

ENV MALLOC_ARENA_MAX=2
ENV JAVA_TOOL_OPTIONS="\
    # Serial GC: lowest overhead for small heaps, single-threaded \
    -XX:+UseSerialGC \
    # Heap max = 60% of the container memory limit (rest is for metaspace, stacks, native) \
    -XX:MaxRAMPercentage=60 \
    # Smaller thread stacks (default is 1MB per thread) \
    -Xss512k \
    # Cap the JIT code cache \
    -XX:ReservedCodeCacheSize=32m \
    # C1 compiler only: less compiler memory/CPU, lower peak throughput \
    -XX:TieredStopAtLevel=1 \
    # Fewer JIT compiler threads \
    -XX:CICompilerCount=2 \
    # Safety cap on class metadata \
    -XX:MaxMetaspaceSize=96m \
    # Exit on OOM so Docker's restart policy can recover cleanly \
    -XX:+ExitOnOutOfMemoryError \
    "

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
