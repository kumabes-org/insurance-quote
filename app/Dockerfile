# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Cache de dependências (otimiza builds subsequentes)
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy AS final
WORKDIR /app

# Criação de usuário não-root para segurança (Princípio do Menor Privilégio)
RUN groupadd -g 10001 appgroup && \
    useradd -u 10001 -g appgroup -s /bin/sh -m appuser

# Instalação de curl para healthchecks (opcional, mas útil)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copia apenas o artefato necessário do estágio de build
COPY --from=build /app/target/*.jar app.jar

# Ajuste de permissões
RUN chown appuser:appgroup app.jar
USER appuser

# Configurações de JVM para containers e Virtual Threads
# -XX:+UseParallelGC é frequentemente eficiente para microserviços curtos,
# mas o G1GC (padrão) lida bem com as heaps maiores que Virtual Threads podem demandar.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

#java -javaagent:opentelemetry-javaagent.jar \
#     -Dotel.exporter.otlp.endpoint=http://otel-collector:4317 \
#     -Dotel.resource.attributes=service.name=minha-app-java21 \
#     -jar app.jar