# ✅ Checklist de Verificación - Liquibase con YAML

## Antes de Ejecutar

### 📁 Archivos de Migración
- [ ] `db.changelog-master.yaml` existe
- [ ] `changes/001-create-initial-schema.yaml` existe
- [ ] `changes/002-insert-initial-data.yaml` existe
- [ ] Los archivos YAML tienen sintaxis correcta (2 espacios de indentación)

### ⚙️ Configuración
- [ ] `pom.xml` configurado correctamente
- [ ] `application.properties` configurado
- [ ] `spring.jpa.hibernate.ddl-auto=none`

### 🔐 Variables de Entorno
- [ ] Archivo `.env` existe y tiene todas las variables
- [ ] `JDBC_DATABASE_URL` configurado
- [ ] `SUPABASE_USER` configurado
- [ ] `SUPABASE_PASS` configurado
- [ ] `JWT_SECRET_KEY` configurado
- [ ] Variables de entorno cargadas en IntelliJ (ver ENV_SETUP.md)

### 🗄️ Base de Datos
- [ ] PostgreSQL está corriendo (o Supabase configurado)
- [ ] Puedes conectarte a la base de datos
- [ ] La base de datos está vacía O estás listo para sincronizar

## Durante la Primera Ejecución

### Compilación
- [ ] `mvn clean install` se ejecuta sin errores
- [ ] Todas las dependencias se descargan correctamente

### Inicio de Aplicación
- [ ] `mvn spring-boot:run` inicia sin errores
- [ ] Ves logs de Liquibase ejecutándose
- [ ] No hay errores de conexión a base de datos
- [ ] No hay errores de sintaxis YAML

### Migraciones
- [ ] Ves: "Running Changeset: changes/001-create-initial-schema.yaml"
- [ ] Ves: "Running Changeset: changes/002-insert-initial-data.yaml"
- [ ] No hay errores de SQL
- [ ] La aplicación inicia completamente

## Después de Ejecutar

### Verificar Base de Datos

Conéctate a tu base de datos y ejecuta:

```sql
-- 1. Verificar tablas de Liquibase
SELECT * FROM databasechangelog ORDER BY dateexecuted DESC;
-- Deberías ver 2 registros (001 y 002)

SELECT * FROM databasechangeloglock;
-- locked debe ser FALSE

-- 2. Verificar tablas creadas
\dt
-- Deberías ver: user, activity_series, legal_process, 
--               user_legal_processes, action, notification, history

-- 3. Verificar usuarios insertados
SELECT COUNT(*) FROM "user";
-- Debería retornar: 14

SELECT document_number, first_name, last_name, email FROM "user" LIMIT 5;
-- Deberías ver datos de usuarios

-- 4. Verificar estructura de una tabla
\d "user"
-- Deberías ver todas las columnas definidas
```

### Verificar Logs

Busca en los logs de la aplicación:

- [ ] ✅ "Liquibase: Successfully acquired change log lock"
- [ ] ✅ "Liquibase: Reading from public.databasechangelog"
- [ ] ✅ "Liquibase: changes/001-create-initial-schema.yaml"
- [ ] ✅ "Liquibase: changes/002-insert-initial-data.yaml"
- [ ] ✅ "Liquibase: Successfully released change log lock"
- [ ] ❌ NO debe haber "ERROR" relacionado con Liquibase

## Crear Tu Primera Migración

### Preparación
- [ ] Decidiste qué cambio hacer (ej: agregar columna `phone`)
- [ ] Revisaste ejemplos en `EXAMPLE-migration-template.yaml`
- [ ] Tienes el nombre: `003-descripcion-corta.yaml`

### Creación
- [ ] Creaste archivo `003-tu-cambio.yaml` en `changes/`
- [ ] El archivo tiene sintaxis YAML correcta
- [ ] Usaste ID único: `003-descripcion-unica`
- [ ] Incluiste tu nombre en `author`

### Inclusión
- [ ] Agregaste `include` en `db.changelog-master.yaml`
- [ ] La ruta es correcta: `changes/003-tu-cambio.yaml`
- [ ] Usaste `relativeToChangelogFile: true`

### Ejecución
- [ ] Ejecutaste `mvn spring-boot:run`
- [ ] Viste en logs: "Running Changeset: changes/003-tu-cambio.yaml"
- [ ] No hubo errores
- [ ] Verificaste el cambio en la base de datos

## Comandos Básicos

```bash
# Compilar proyecto
mvn clean compile

# Ejecutar aplicación
mvn spring-boot:run

# Empaquetar JAR
mvn clean package
```

## Problemas Comunes

### ❌ Error: Variables de entorno no cargadas
- [ ] Verificaste que IntelliJ tiene configurado el .env
- [ ] Reiniciaste IntelliJ después de configurar
- [ ] Las variables están en formato correcto: `KEY=value`

### ❌ Error: Base de datos no conecta
- [ ] Verificaste las credenciales de base de datos
- [ ] La URL de conexión es correcta
- [ ] El servicio de PostgreSQL está corriendo

## Documentación Consultada

- [ ] Leí `README.md` - Documentación general
- [ ] Leí `ENV_SETUP.md` - Variables de entorno
- [ ] Revisé `QUICKSTART.md` - Inicio rápido

## Integración con Equipo

- [ ] Archivos de migración commiteados al repositorio
- [ ] `.env` está en `.gitignore` (NO commitear)
- [ ] `.env.example` está actualizado
- [ ] Documentación compartida con el equipo
- [ ] Equipo sabe cómo ejecutar migraciones

## Producción

⚠️ **ANTES DE LLEVAR A PRODUCCIÓN:**

- [ ] Todas las migraciones probadas en desarrollo
- [ ] Todas las migraciones probadas en staging
- [ ] Backup de base de datos de producción realizado
- [ ] Plan de rollback preparado
- [ ] Equipo notificado del deployment
- [ ] Migraciones ejecutadas en horario de bajo tráfico

## Recursos

### Archivos de Ayuda
- `README.md` - Documentación general  
- `QUICKSTART.md` - Inicio rápido
- `ENV_SETUP.md` - Configurar variables de entorno
- `GMAIL_SMTP_SETUP.md` - Configuración de correo

---

## ✅ Todo Verificado

Si marcaste todos los checkboxes principales, ¡estás listo para ejecutar la aplicación!

**Siguiente paso:** Ejecutar `mvn spring-boot:run` y comenzar a usar la API.

---

**Última actualización:** 2025-11-23

