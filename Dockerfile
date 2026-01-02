FROM openjdk:17-jdk-slim
WORKDIR /app
COPY sistema.jar app.jar
EXPOSE 8080
<<<<<<< HEAD
ENTRYPOINT ["java", "-Xmx512M", "-jar", "app.jar"]
=======
ENTRYPOINT ["java", "-Xmx512M", "-jar", "app.jar"]
>>>>>>> 0327634cd2a019558b413b4580b0990d1ebe0524
