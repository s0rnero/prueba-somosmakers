# syntax=docker/dockerfile:1.7

FROM maven:3.10.0-eclipse-temurin-25-alpine AS build
WORKDIR /workspace/backend

COPY backend/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests dependency:go-offline

COPY backend/src ./src
COPY database/migrations ../database/migrations
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre-alpine AS runtime

RUN apk add --no-cache curl \
	&& addgroup -S app \
	&& adduser -S app -G app

WORKDIR /app
COPY --from=build /workspace/backend/target/backend-*.jar app.jar
RUN chown -R app:app /app

USER app
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
