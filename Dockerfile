FROM eclipse-temurin:21-jdk AS java-build
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
COPY countries-mcp-server/build.gradle.kts ./countries-mcp-server/build.gradle.kts
COPY src ./src

RUN chmod +x gradlew && ./gradlew :bootJar --no-daemon -x test

FROM node:22-bookworm-slim AS node-build
WORKDIR /weather-mcp

COPY weather-mcp/package.json weather-mcp/package-lock.json ./
RUN npm ci

COPY weather-mcp/src ./src
COPY weather-mcp/tsconfig.json ./

FROM eclipse-temurin:21-jre AS final
RUN apt-get update \
	&& apt-get install -y --no-install-recommends nodejs npm \
	&& rm -rf /var/lib/apt/lists/*

RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app

COPY --from=java-build /workspace/build/libs/app.jar app.jar
COPY --from=node-build /weather-mcp ./weather-mcp
COPY ai_flow/data ./ai_flow/data

RUN chown -R app:app /app

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
