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

# Copy the built artifact from the build stage
COPY --from=build /app/target/*.jar app.jar

# Copy the Lucene index directory
COPY nutrigraph/lucene-index-african /app/lucene-index-african

# Create a directory for images
RUN mkdir -p /app/images

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=docker

# Expose the port the app runs on
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "app.jar"]
