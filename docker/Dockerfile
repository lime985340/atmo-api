# syntax=docker/dockerfile:1.6
FROM gradle:8.10-jdk17 AS build
WORKDIR /src
COPY . .
# NOTE: This repo intentionally does not ship with Gradle Wrapper (gradlew).
# Use the Gradle binary provided by the build image.
RUN gradle :api:bootJar :ingestion:bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/api/build/libs/*.jar /app/app.jar
COPY --from=build /src/ingestion/build/libs/*.jar /app/ingestion.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
