FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:11-jre
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=1010
ENV APP_UPLOAD_DIR=/app/uploads

COPY --from=build /app/target/*.jar /app/app.jar

RUN mkdir -p /app/uploads

EXPOSE 1010

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
