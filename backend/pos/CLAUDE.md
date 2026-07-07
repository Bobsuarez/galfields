# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 4.1.0 backend for a POS (point of sale) system, part of the larger Galfields project. Java 21, built with Gradle.

Base package: `co.com.galfields.pos`. Entry point: `src/main/java/co/com/galfields/pos/PosApplication.java`.

The project is currently a fresh Spring Initializr skeleton — no controllers, services, entities, or tests have been added yet. As functionality is built out, prefer conventional Spring Boot layering (`controller` / `service` / `repository` / `entity` or `model` packages under `co.com.galfields.pos`).

## Commands

Use the Gradle wrapper (`./gradlew`), not a system-installed `gradle`.

- Build: `./gradlew build`
- Run the app: `./gradlew bootRun`
- Run all tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests "co.com.galfields.pos.SomeTestClass"`
- Run a single test method: `./gradlew test --tests "co.com.galfields.pos.SomeTestClass.someMethod"`
- Clean build output: `./gradlew clean`

Tests use JUnit 5 (JUnit Platform).

## Dependencies of note

- **Spring Data JPA** (`spring-boot-starter-data-jpa`) — persistence layer.
- **Spring RestClient** (`spring-boot-starter-restclient`) — for outbound HTTP calls.
- **Lombok** — available at compile time only (`compileOnly`/`annotationProcessor`); use annotations like `@Getter`/`@Setter`/`@Data` instead of hand-written boilerplate.
- **PostgreSQL** driver at runtime — the intended production/dev database.
- **SQLite** driver also present at runtime — check `application.properties` (or `application-<profile>.properties`, none exist yet) to see which datasource is actually active before assuming Postgres.

Corresponding test-scoped starters (`spring-boot-starter-data-jpa-test`, `spring-boot-starter-restclient-test`) are included for integration testing against JPA repositories and REST clients.

## Configuration

`src/main/resources/application.properties` currently only sets `spring.application.name=pos`. No datasource, port, or profile configuration exists yet — add it here (or in profile-specific `application-<profile>.properties` files) as the project grows.
