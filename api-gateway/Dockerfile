# API Gateway
#
# Multi-stage build: the first stage compiles the jar with Maven, the second
# runs it on a small JRE image as a non-root user. No local Java or Maven
# installation is needed to build this image.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# A fixed heap keeps several parallel image builds within Docker's memory.
ENV MAVEN_OPTS="-Xmx512m"
COPY pom.xml .
COPY src ./src
# The cache mount keeps downloaded dependencies between builds.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER app
EXPOSE 8080
# Size the heap from the container memory limit instead of the host's RAM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
