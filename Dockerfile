# Stage 1: Build the application
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Copia todo el proyecto
COPY . .

# Da permisos al wrapper
RUN chmod +x mvnw

# Compila el proyecto (usa el wrapper Maven del repo)
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copia el JAR generado
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
