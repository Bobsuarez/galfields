# Multi-module Gradle build (Bancolombia Clean Architecture scaffold) — unlike
# backend/pos's single module, every domain/infrastructure/applications
# subproject needs to be in the build context for :app-service:bootJar to
# resolve its project(':...') dependencies.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradle gradle
COPY gradlew build.gradle settings.gradle main.gradle gradle.properties lombok.config ./
COPY applications applications
COPY domain domain
COPY infrastructure infrastructure

# validateStructure (Bancolombia Clean Architecture plugin) miscomputes its
# own file paths when the project root is shallow (e.g. /app in a Docker
# build) - throws NoSuchFileException on a mangled path. Not needed for a
# release build: it's an architecture-layering lint already enforced by
# ./gradlew build in CI/local dev, not something the runtime jar needs.
RUN ./gradlew --no-daemon :app-service:bootJar -x test -x pitest -x jacocoTestReport -x validateStructure

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/applications/app-service/build/libs/pos_transactions.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
