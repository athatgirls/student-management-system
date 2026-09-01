FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B package

FROM eclipse-temurin:11-jre
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=1010
ENV APP_UPLOAD_DIR=/app/uploads

COPY --from=build /app/target/*.jar /app/app.jar

RUN groupadd --system mis \
    && useradd --system --gid mis --home-dir /app --shell /usr/sbin/nologin mis \
    && mkdir -p /app/uploads \
    && chown -R mis:mis /app

USER mis

EXPOSE 1010

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=5 \
  CMD curl --fail --silent http://127.0.0.1:1010${SERVER_CONTEXT_PATH:-/SCSE@hbut}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
