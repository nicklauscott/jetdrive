
# Stage 1: Build
FROM gradle:8.0.2-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test

# Stage 2: Run
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]


COPY build/libs/jetdrive-0.0.1-SNAPSHOT.jar app.jar