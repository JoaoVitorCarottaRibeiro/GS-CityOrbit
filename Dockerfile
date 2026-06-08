# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Cache de dependências (só re-baixa se o pom.xml mudar)
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Compila o projeto
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copia o JAR gerado
COPY --from=build /app/target/cityorbit-*.jar app.jar

# Porta que o Spring Boot escuta
EXPOSE 8080

# Limites de memória adequados para o free tier do Render (512 MB)
ENTRYPOINT ["java", \
  "-Xmx384m", \
  "-Xms128m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
