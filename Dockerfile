# === Build Stage ===
FROM gradle:8.4.0-jdk21-alpine AS build

WORKDIR /app

COPY gradle ./gradle
COPY build.gradle settings.gradle gradlew ./
COPY src ./src

RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# === Run Stage ===
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
