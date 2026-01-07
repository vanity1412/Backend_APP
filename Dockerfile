# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first (cached layer)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -q

# Run stage - Use slim JRE image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Set timezone to Vietnam (Ho Chi Minh)
ENV TZ=Asia/Ho_Chi_Minh
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone

# Add non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy jar file
COPY --from=build /app/target/*.jar app.jar

# Copy assets folder (consider moving to CDN later)
COPY assets ./assets

# Change ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose port
EXPOSE 8080

# Optimized JVM flags for containers
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=100", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=Asia/Ho_Chi_Minh", \
    "-jar", "app.jar"]
