# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /workspace/target/incident-tracker-api-*.jar app.jar
USER appuser
EXPOSE 8082
HEALTHCHECK --interval=15s --timeout=3s --retries=5 \
  CMD wget -qO- http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
