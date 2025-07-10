# === Build Stage ===
FROM gradle:8.4.0-jdk21-alpine AS build

WORKDIR /app

COPY gradle ./gradle
COPY build.gradle settings.gradle gradlew ./  # 너에겐 이 두 파일이 있으니 이렇게 작성
COPY src ./src

RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# === Run Stage ===
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
