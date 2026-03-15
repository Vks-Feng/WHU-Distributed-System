FROM maven:3.9.11-eclipse-temurin-17

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/target/sec-kill-0.0.1-SNAPSHOT.jar"]
