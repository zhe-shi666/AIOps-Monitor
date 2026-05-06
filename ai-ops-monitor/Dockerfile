FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar /app/aiops-monitor.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/aiops-monitor.jar"]
