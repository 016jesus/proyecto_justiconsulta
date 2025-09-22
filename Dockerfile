# ====== Builder stage (Temurin 25 + Maven) ======
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /workspace

# Copiar pom y wrapper si existe (mejora cache)
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN echo ">>> MAVEN: prefetch dependencies" && mvn -q -B -DskipTests dependency:go-offline

# Copiar el resto del código
COPY src/ src/
COPY pom.xml .

# Ejecutar build con logs verbosos
RUN echo ">>> MAVEN: package (this may take a while)" \
 && mvn -B -DskipTests package || (echo "MAVEN BUILD FAILED"; ls -la target || true; exit 1)

# Asegurar que el JAR existe
RUN echo ">>> target contents:" && ls -la target

# ====== Runtime stage (Debian slim + Temurin 25 JRE) ======
FROM eclipse-temurin:25-jre-jammy AS runtime
WORKDIR /app

# Instalar dumb-init
RUN apt-get update \
 && apt-get install -y --no-install-recommends dumb-init ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# Crear usuario no-root
RUN groupadd --gid 1000 spring && useradd --uid 1000 --gid 1000 --create-home --home-dir /home/spring spring \
 && mkdir -p /app/logs /home/spring && chown -R spring:spring /app /home/spring

# Copiar jar desde builder (usa nombre exacto si lo conoces)
COPY --from=builder --chown=spring:spring /workspace/target/*.jar /app/app.jar

USER spring:spring
ENV JAVA_OPTS=""
ENV TZ=UTC

EXPOSE 8080
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -XX:MaxRAMPercentage=75.0 -jar /app/app.jar --server.port=${PORT:-8080}"]
