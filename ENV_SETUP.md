# Configuración de Variables de Entorno

Este proyecto utiliza variables de entorno para configurar aspectos sensibles y específicos del entorno de ejecución.

## Configuración en IntelliJ IDEA

### Opción 1: EnvFile Plugin (Recomendado)

1. **Instalar el plugin EnvFile**:
   - Ve a `File` → `Settings` → `Plugins`
   - Busca "EnvFile"
   - Instala el plugin y reinicia el IDE

2. **Configurar el archivo .env**:
   - Ve a `Run` → `Edit Configurations`
   - Selecciona tu configuración de ejecución (JusticonsultaApplication)
   - En la pestaña "EnvFile", haz clic en el botón `+`
   - Selecciona tu archivo `.env`
   - Marca la opción "Enable EnvFile"
   - Aplica y guarda

### Opción 2: Variables de Entorno Manuales

1. **Configurar en Run/Debug Configuration**:
   - Ve a `Run` → `Edit Configurations`
   - Selecciona tu configuración de ejecución (JusticonsultaApplication)
   - En "Environment variables", haz clic en el icono de carpeta
   - Copia todas las variables del archivo `.env` en formato `KEY=value`
   - Aplica y guarda

### Opción 3: application.properties con valores por defecto

Para desarrollo local, puedes crear un archivo `application-local.properties`:

```properties
# Copiar de .env y pegar aquí
PORT=8080
JDBC_DATABASE_URL=jdbc:postgresql://...
# etc.
```

Luego ejecuta la aplicación con el perfil local:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Variables Requeridas

Asegúrate de tener configuradas todas estas variables:

- `PORT` - Puerto del servidor (default: 8080)
- `JDBC_DATABASE_URL` - URL de conexión a PostgreSQL
- `SUPABASE_USER` - Usuario de Supabase
- `SUPABASE_PASS` - Contraseña de Supabase
- `SUPABASE_URL` - URL de tu proyecto Supabase
- `SUPABASE_SERVICE_ROLE_KEY` - Service role key de Supabase
- `JWT_SECRET_KEY` - Clave secreta para JWT
- `API_SECRET_KEY` - Clave secreta para API
- `GMAIL_USERNAME` - Email de Gmail para SMTP
- `GMAIL_APP_PASSWORD` - Contraseña de aplicación de Gmail
- `APP_URL` - URL de la aplicación (default: http://localhost:8080)

## Verificación

Para verificar que las variables se están cargando correctamente, revisa los logs al iniciar la aplicación.
Si hay variables faltantes, verás errores relacionados con la configuración.

## Seguridad

⚠️ **NUNCA** commitees el archivo `.env` al repositorio. 
Este archivo debe estar en `.gitignore`.

Para nuevos desarrolladores, proporciona un archivo `.env.example` con valores de ejemplo.

