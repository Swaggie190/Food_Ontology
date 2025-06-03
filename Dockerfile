# Multi-stage build for NutriGraph application
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy the Maven POM and source code
COPY nutrigraph/pom.xml .
COPY nutrigraph/src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the built artifact from the build stage
COPY --from=build /app/target/*.jar app.jar

# Copy the Lucene index directory
COPY nutrigraph/lucene-index-african /app/lucene-index-african

# Create directories for images and ensure proper permissions
RUN mkdir -p /app/images && chmod 755 /app/images

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xmx2g -XX:+UseG1GC"

# Add a wait script for service dependencies
COPY wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

# Expose the port the app runs on
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Command to run the application with wait for Fuseki
CMD ["/wait-for-it.sh", "fuseki:3030", "--timeout=60", "--", "java", "-jar", "/app/app.jar"]