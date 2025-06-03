# Multi-stage build with Maven, Spring Boot, and Fuseki

# Stage 1: Build the Spring Boot application
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy the Maven POM and source code
COPY nutrigraph/pom.xml .
COPY nutrigraph/src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Fuseki Server preparation
FROM openjdk:11-jre-slim as fuseki

# Install required packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget unzip && \
    rm -rf /var/lib/apt/lists/*

# Set up working directory for Fuseki
WORKDIR /fuseki

# Copy Fuseki files from the local directory
COPY apache-jena-fuseki-5.4.0/fuseki-server.jar /fuseki/
COPY apache-jena-fuseki-5.4.0/log4j2.properties /fuseki/

# Copy configuration and ontology files
COPY apache-jena-fuseki-5.4.0/run/config.ttl /fuseki/run/
COPY apache-jena-fuseki-5.4.0/run/configuration/ /fuseki/run/configuration/

# Create data directories
RUN mkdir -p /fuseki/run/databases/Diseases
RUN mkdir -p /fuseki/run/databases/african-middle-eastern-kg

# Final runtime image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built Spring Boot artifact from the build stage
COPY --from=build /app/target/*.jar app.jar

# Copy the Lucene index directory
COPY nutrigraph/lucene-index-african /app/lucene-index-african

# Copy Fuseki from the fuseki stage
COPY --from=fuseki /fuseki /fuseki

# Create a directory for images
RUN mkdir -p /app/images

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=docker

# Create a startup script
RUN echo '#!/bin/bash\n\
# Start Fuseki server in the background\n\
cd /fuseki && java -jar fuseki-server.jar --config=run/config.ttl &\n\
\n\
# Wait for Fuseki to start\n\
echo "Waiting for Fuseki to start..."\n\
sleep 10\n\
\n\
# Start the Spring Boot application\n\
cd /app\n\
echo "Starting Spring Boot application..."\n\
java -jar app.jar\n\
' > /start.sh && chmod +x /start.sh

# Expose ports for both Spring Boot and Fuseki
EXPOSE 8080 3030

# Set the entry point to use the startup script
ENTRYPOINT ["/start.sh"]