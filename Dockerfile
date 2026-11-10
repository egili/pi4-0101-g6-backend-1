FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/backend-1-0.0.1-SNAPSHOT.jar app.jar
RUN addgroup -S app && adduser -S app -G app
USER app
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]