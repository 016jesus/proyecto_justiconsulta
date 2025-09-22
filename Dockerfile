# ====== Builder stage (Alpine) ======
FROM maven:3.9-eclipse-temurin-25-alpine AS builder
WORKDIR /workspace

# Copia archivos mínimos para resolver dependencias
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline

# Copia el código fuente y compila
COPY src/ src/
RUN mvn -q -B -DskipTests package

# ====== Runtime stage (Alpine, non-root) ======
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Crear usuario no root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Variables de entorno configurables
ENV JAVA_OPTS=""
ENV SERVER_PORT=8080

# Secrets/props típicas de este proyecto (sobre-escríbelas en despliegue)
ENV SECURITY_SUPABASE_JWT_SECRET=""
ENV SECURITY_SUPABASE_ISSUER=""
ENV SECURITY_API_SECRET_KEY=""
ENV API_EXTERNAL_BASE_URL=""

# Copia el JAR construido desde el builder
COPY --from=builder /workspace/target/*.jar /app/app.jar

EXPOSE 8080

# Arranque de la app (permite inyectar JAVA_OPTS y cambiar el puerto)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --server.port=${SERVER_PORT}"]