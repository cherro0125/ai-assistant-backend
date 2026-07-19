FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
COPY countries-mcp-server/build.gradle.kts ./countries-mcp-server/build.gradle.kts
COPY countries-mcp-server/src ./countries-mcp-server/src

RUN chmod +x gradlew && ./gradlew :countries-mcp-server:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app

COPY --from=build /workspace/countries-mcp-server/build/libs/app.jar app.jar
RUN chown app:app app.jar

USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
