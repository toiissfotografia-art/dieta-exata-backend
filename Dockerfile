FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# Usamos o asterisco para garantir que ele ache o arquivo mesmo com variações de nome
COPY sistema*.jar app.jar
EXPOSE 8081
# Variáveis para garantir que o Spring Boot use a porta e banco corretos
ENV SERVER_PORT=8081
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://nozomi.proxy.rlwy.net:24769/railway
ENTRYPOINT ["java", "-Xmx512M", "-jar", "app.jar"]