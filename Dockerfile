FROM openjdk:17-jdk-slim
WORKDIR /app
COPY sistema.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512M", "-jar", "app.jar"]