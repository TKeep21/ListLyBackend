FROM gradle:8.11.0-jdk21 AS builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradle.properties ./

RUN gradle dependencies --no-daemon || true

COPY src ./src

RUN gradle build --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*-all.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]