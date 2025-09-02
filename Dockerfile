# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
#ускоряет повторные сборки. Если BuildKit не включен — просто убераем эти два флага, всё равно будет работать.
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package \
 && cp "$(ls -1 target/*.jar | grep -v original | head -n 1)" app.jar

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar /app/app.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java","-jar","/app/app.jar"]