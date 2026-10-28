FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S eco && adduser -S eco -G eco
COPY --from=build /app/target/ecoapi-1.0.0-SNAPSHOT.jar app.jar
USER eco
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
