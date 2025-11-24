e# proyecto_justiconsulta
Proyecto académico del curso Ingeniería de software 2. Enfocado en el desarrollo web y móvil.

## Tecnologías

- **Java 21**
- **Spring Boot 3.5.6**
- **PostgreSQL** (Supabase)
- **Liquibase** - Control de versiones de base de datos
- **Maven** - Gestión de dependencias
- **Spring Security** - Autenticación y autorización
- **JWT** - Tokens de autenticación

## Configuración del Proyecto

### 1. Variables de Entorno

Este proyecto requiere variables de entorno para funcionar. Consulta [ENV_SETUP.md](ENV_SETUP.md) para instrucciones detalladas sobre cómo configurarlas en IntelliJ IDEA.

Copia el archivo `.env.example` a `.env` y configura tus valores:

```bash
cp .env.example .env
```

### 2. Base de Datos y Migraciones

El proyecto utiliza **Liquibase** para gestionar las migraciones de base de datos de forma controlada y versionada.

#### Estructura de Migraciones

```
src/main/resources/db/changelog/
├── db.changelog-master.yaml         # Archivo maestro que incluye todos los changesets
└── changes/
    ├── 001-create-initial-schema.yaml  # Creación del esquema inicial
    └── 002-insert-initial-data.yaml    # Datos iniciales
```

#### Ejecutar Migraciones



### 3. Compilar y Ejecutar

```bash
# Compilar el proyecto
mvn clean install

# Ejecutar la aplicación
mvn spring-boot:run

# O ejecutar el JAR generado
java -jar target/store-0.0.1-SNAPSHOT.jar
```

### 4. Documentación de API

Una vez iniciada la aplicación, la documentación de la API está disponible en:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Estructura del Proyecto

```
src/main/java/com/justiconsulta/store/
├── config/              # Configuraciones (Security, JWT, Mail, etc.)
├── controller/          # Controladores REST
├── dto/                 # DTOs (request/response)
├── exception/           # Manejo de excepciones
├── model/              # Entidades JPA
├── repository/         # Repositorios Spring Data
├── security/           # Componentes de seguridad
└── service/            # Servicios de negocio
    ├── contract/       # Interfaces de servicios
    └── impl/           # Implementaciones de servicios
```

## Esquema de Base de Datos

### Tablas Principales

- **user** - Usuarios del sistema
- **legal_process** - Procesos legales
- **user_legal_processes** - Asociación usuarios-procesos
- **action** - Acciones sobre procesos
- **notification** - Notificaciones a usuarios
- **activity_series** - Series de actividades
- **history** - Historial de consultas

Ver el archivo `001-create-initial-schema.yaml` para el esquema completo.

## Seguridad

El proyecto utiliza:
- **JWT** para autenticación stateless
- **Supabase Auth** para gestión de usuarios
- **Spring Security** para autorización
- **API Keys** para endpoints públicos

## Correo Electrónico

Configurado con Gmail SMTP para envío de notificaciones. 
Ver [GMAIL_SMTP_SETUP.md](GMAIL_SMTP_SETUP.md) para configuración.

## Desarrollo

### Entorno de Desarrollo

1. Java 21 o superior
2. Maven 3.6+
3. PostgreSQL 12+ (o cuenta de Supabase)
4. IntelliJ IDEA (recomendado)

### Plugin Recomendado para IntelliJ

**EnvFile Plugin** - Para cargar automáticamente el archivo `.env`

## Licencia

Ver archivo [LICENSE](LICENSE)

