FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy only what's needed first
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Now copy the actual source code
COPY . .

# Build the JAR
RUN ./gradlew clean bootJar --no-daemon

# ---------- Stage 2 ----------
FROM openjdk:21-jdk-slim AS runner

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
