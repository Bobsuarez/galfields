# El repo no trae el script gradlew (solo gradle-wrapper.jar y .properties),
# asi que invocamos el wrapper directamente via java -cp en vez de ./gradlew.
# Esto tambien evita tener que fijar una version exacta de imagen gradle:X-jdkY.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
    --no-daemon bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
