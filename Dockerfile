FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY sistema.jar app.jar
# Alterado para 8081 conforme sua configuração
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx512M", "-jar", "app.jar"]