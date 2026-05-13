
FROM gradle:8.11.0-jdk21 AS builder

WORKDIR /app


COPY build.gradle.kts settings.gradle.kts gradle.properties ./


RUN gradle dependencies --no-daemon || true


COPY src ./src


RUN gradle build --no-daemon


FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app


COPY --from=builder /app/build/libs/*-all.jar app.jar


ENV MONGO_URI="mongodb://listly_admin:secret123@mongo:27017/listlydb?authSource=admin"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
