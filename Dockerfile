# syntax=docker/dockerfile:1

# One build for both services: docker build --target dispatcher|node .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY common/pom.xml common/
COPY dispatcher/pom.xml dispatcher/
COPY node/pom.xml node/
COPY common/src common/src
COPY dispatcher/src dispatcher/src
COPY node/src node/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
RUN useradd --system --uid 10001 app
WORKDIR /app
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM runtime AS dispatcher
COPY --from=build /src/dispatcher/target/dispatcher.jar app.jar

FROM runtime AS node
COPY --from=build /src/node/target/node.jar app.jar
