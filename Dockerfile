# ---- Build Stage ----
FROM gradle:9.1-jdk21-alpine AS build
WORKDIR /app

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

# Run the application
CMD ["java", "-jar", "app.jar"]
