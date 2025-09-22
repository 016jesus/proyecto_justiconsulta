# ====== Builder stage (full Maven + Temurin 25) ======
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /workspace

# Copia solo lo mínimo para cachear dependencias
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN mvn -q -B -DskipTests dependency:go-offline

# Copia el código fuente y empaqueta (sin tests para velocidad; ajusta si quieres tests)
COPY src/ src/
RUN mvn -q -B -DskipTests package

# ====== Runtime stage (Debian slim, non-root, using temurin 25 jre) ======
FROM eclipse-temurin:25-jre-jammy AS runtime
WORKDIR /app

# Instalar dumb-init para manejo de señales (ligero y recomendado)
# y limpiar caches para mantener la imagen pequeña
RUN apt-get update \
 && apt-get install -y --no-install-recommends dumb-init ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# Crear usuario no-root
RUN groupadd --gid 1000 spring && \
    useradd --uid 1000 --gid 1000 --create-home --home-dir /home/spring spring && \
    mkdir -p /app/logs && chown -R spring:spring /app /home/spring

USER spring:spring

# Variables de entorno configurables (Render expone $PORT)
ENV JAVA_OPTS=""
ENV SERVER_PORT=8080
ENV TZ=UTC

# Secrets/props sensibles: NO poner valores reales aquí. Poner en Render dashboard.
ENV SECURITY_SUPABASE_JWT_SECRET=""
ENV SECURITY_SUPABASE_ISSUER=""
ENV SECURITY_API_SECRET_KEY=""
ENV API_EXTERNAL_BASE_URL=""

# Copiar JAR construido desde el stage builder
# Si tu build genera un -exec.jar u otro nombre, cambia el wildcard por el nombre exacto
COPY --from=builder --chown=spring:spring /workspace/target/*.jar /app/app.jar

# Puerto (Render usa $PORT env var pero dejamos EXPOSE para documentación)
EXPOSE 8080

# Recomendación de flags JVM container-aware y memoria % (ajusta según necesidades)
# -XX:+UseContainerSupport es activado por defecto en JDK 25, pero dejamos opciones útiles:
# -XX:MaxRAMPercentage controla cuánto RAM del contenedor puede usar la JVM (p. ej. 75%).
# Puedes inyectar/reemplazar JAVA_OPTS en el dashboard de Render si necesitas más tuning.
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication -jar /app/app.jar --server.port=${PORT:-8080}"]
