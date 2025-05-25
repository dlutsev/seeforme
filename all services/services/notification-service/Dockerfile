FROM gradle:8.13.0-jdk21-alpine AS build

WORKDIR /notification-service
COPY src src
COPY build.gradle .
COPY settings.gradle .

RUN gradle build

FROM openjdk:21

COPY --from=build /notification-service/build/libs/notification-service-0.0.1-SNAPSHOT.jar .

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "notification-service-0.0.1-SNAPSHOT.jar", "--server.port=8080"]