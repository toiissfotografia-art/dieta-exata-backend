FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY sistema.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx512M", "-jar", "app.jar"]