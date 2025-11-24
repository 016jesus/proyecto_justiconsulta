# 📦 Guía de Empaquetado JAR con Liquibase

## 🎯 Respuesta Rápida

**SÍ**, los archivos de migración de Liquibase **se incluyen automáticamente** en el JAR.

### ¿Por qué?

Los archivos están en `src/main/resources/`, y Maven empaqueta automáticamente todo lo que esté en esta carpeta dentro del JAR.

---

## 📁 Estructura que se Incluye en el JAR

```
src/main/resources/               → Se incluye TODO esto en el JAR
├── application.properties         ✅ En el JAR
├── static/                        ✅ En el JAR
│   └── index.html
└── db/                            ✅ En el JAR (IMPORTANTE)
    └── changelog/
        ├── db.changelog-master.yaml
        └── changes/
            ├── 001-create-initial-schema.yaml
            ├── 002-insert-initial-data.yaml
            └── EXAMPLE-migration-template.yaml
```

### Dentro del JAR quedará así:

```
store-0.0.1-SNAPSHOT.jar
├── BOOT-INF/
│   ├── classes/
│   │   ├── application.properties       ✅
│   │   ├── db/                          ✅ Migraciones aquí
│   │   │   └── changelog/
│   │   │       ├── db.changelog-master.yaml
│   │   │       └── changes/
│   │   │           ├── 001-create-initial-schema.yaml
│   │   │           └── 002-insert-initial-data.yaml
│   │   └── com/                         (clases compiladas)
│   └── lib/                             (dependencias)
│       ├── liquibase-core-x.x.x.jar     ✅
│       └── ...
└── META-INF/
```

---

## 🔨 Cómo Construir el JAR

### Opción 1: JAR Ejecutable (Recomendado)

```bash
# Limpiar y construir
mvn clean package

# O sin ejecutar tests
mvn clean package -DskipTests
```

**Resultado:** `target/store-0.0.1-SNAPSHOT.jar`

### Opción 2: Usando Spring Boot Maven Plugin

```bash
mvn clean spring-boot:repackage
```

### Opción 3: Install (construye + instala en repositorio local)

```bash
mvn clean install
```

---

## ✅ Verificar que las Migraciones Están en el JAR

### Método 1: Listar Contenido del JAR

```bash
# Windows PowerShell
jar -tf target/store-0.0.1-SNAPSHOT.jar | Select-String "db/changelog"

# Linux/Mac
jar -tf target/store-0.0.1-SNAPSHOT.jar | grep "db/changelog"
```

**Deberías ver:**
```
BOOT-INF/classes/db/changelog/db.changelog-master.yaml
BOOT-INF/classes/db/changelog/changes/001-create-initial-schema.yaml
BOOT-INF/classes/db/changelog/changes/002-insert-initial-data.yaml
BOOT-INF/classes/db/changelog/changes/EXAMPLE-migration-template.yaml
```

### Método 2: Extraer y Revisar

```bash
# Extraer el JAR
jar -xf target/store-0.0.1-SNAPSHOT.jar

# Ver archivos de migración
ls BOOT-INF/classes/db/changelog/
```

### Método 3: Usar 7-Zip o WinRAR (Windows)

1. Abre `target/store-0.0.1-SNAPSHOT.jar` con 7-Zip
2. Navega a `BOOT-INF/classes/db/changelog/`
3. Verifica que estén todos los archivos YAML

---

## 🚀 Ejecutar el JAR

### Con Variables de Entorno

```bash
# Windows PowerShell
java -jar target/store-0.0.1-SNAPSHOT.jar

# O especificando puerto
java -jar -Dserver.port=8080 target/store-0.0.1-SNAPSHOT.jar

# Con perfil específico
java -jar -Dspring.profiles.active=prod target/store-0.0.1-SNAPSHOT.jar
```

### Con Archivo .env Externo

**Opción 1: Variables de entorno del sistema**

```bash
# Windows PowerShell
$env:JDBC_DATABASE_URL="jdbc:postgresql://..."
$env:SUPABASE_USER="postgres.xxx"
$env:SUPABASE_PASS="your-password"
java -jar target/store-0.0.1-SNAPSHOT.jar
```

**Opción 2: Archivo de propiedades externo**

```bash
# Crear application.properties externo
# Colocarlo en el mismo directorio que el JAR
java -jar target/store-0.0.1-SNAPSHOT.jar --spring.config.location=file:./application.properties
```

**Opción 3: Variables como argumentos**

```bash
java -jar target/store-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url="jdbc:postgresql://..." \
  --spring.datasource.username="postgres.xxx" \
  --spring.datasource.password="your-password"
```

---

## 📋 Checklist de Deployment

### Antes de Construir el JAR

- [ ] Todas las migraciones funcionan en desarrollo
- [ ] `application.properties` tiene valores correctos o usa variables
- [ ] `spring.liquibase.enabled=true`
- [ ] `spring.jpa.hibernate.ddl-auto=none`
- [ ] Tests pasan correctamente
- [ ] Código commiteado a Git

### Construir el JAR

```bash
# 1. Limpiar builds anteriores
mvn clean

# 2. Ejecutar tests
mvn test

# 3. Construir JAR
mvn package -DskipTests
```

- [ ] JAR construido sin errores
- [ ] JAR está en `target/store-0.0.1-SNAPSHOT.jar`
- [ ] Tamaño del JAR es razonable (~50-100 MB)

### Verificar el JAR

```bash
# Verificar migraciones incluidas
jar -tf target/store-0.0.1-SNAPSHOT.jar | Select-String "db/changelog"
```

- [ ] Migraciones encontradas en el JAR
- [ ] `db.changelog-master.yaml` presente
- [ ] Archivos de changes presentes

### Probar Localmente

```bash
# Ejecutar JAR localmente
java -jar target/store-0.0.1-SNAPSHOT.jar
```

- [ ] Aplicación inicia sin errores
- [ ] Liquibase ejecuta migraciones
- [ ] Conexión a base de datos exitosa
- [ ] Endpoints funcionan correctamente

---

## 🎯 Deployment en Diferentes Ambientes

### Desarrollo Local

```bash
# Usar variables del .env o configuración local
java -jar target/store-0.0.1-SNAPSHOT.jar
```

### Staging

```bash
# Con variables de entorno de staging
java -jar \
  -Dspring.profiles.active=staging \
  -DJDBC_DATABASE_URL="jdbc:postgresql://staging-db..." \
  target/store-0.0.1-SNAPSHOT.jar
```

### Producción

```bash
# Con variables de entorno de producción
java -jar \
  -Dspring.profiles.active=prod \
  -Xms512m \
  -Xmx1024m \
  -DJDBC_DATABASE_URL="jdbc:postgresql://prod-db..." \
  -DSUPABASE_USER="..." \
  -DSUPABASE_PASS="..." \
  target/store-0.0.1-SNAPSHOT.jar
```

**Con Docker:**

```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/store-0.0.1-SNAPSHOT.jar app.jar

# Las migraciones están DENTRO del JAR
# No necesitas copiar archivos adicionales

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 🔄 Liquibase en Producción

### Primera Vez (Base de Datos Vacía)

Cuando ejecutes el JAR por primera vez:

1. ✅ Liquibase crea las tablas de control
2. ✅ Ejecuta todas las migraciones en orden
3. ✅ Registra en `databasechangelog`
4. ✅ Aplicación lista para usar

### Actualizaciones (Base de Datos Existente)

Cuando actualices el JAR con nuevas migraciones:

1. ✅ Liquibase detecta migraciones nuevas
2. ✅ Ejecuta solo las que faltan
3. ✅ No re-ejecuta las anteriores
4. ✅ Actualización transparente

### Logs de Liquibase

Verás en los logs:

```
Liquibase: Successfully acquired change log lock
Liquibase: Reading from public.databasechangelog
Liquibase: classpath:db/changelog/db.changelog-master.yaml: ...
Liquibase: Successfully released change log lock
```

---

## ⚠️ Problemas Comunes

### ❌ Error: "Cannot find changelog file"

**Causa:** Ruta incorrecta en `application.properties`

**Solución:**
```properties
# CORRECTO
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml

# INCORRECTO
spring.liquibase.change-log=file:db/changelog/db.changelog-master.yaml
spring.liquibase.change-log=db/changelog/db.changelog-master.yaml
```

### ❌ Migraciones no se incluyen en el JAR

**Causa:** Archivos fuera de `src/main/resources`

**Solución:** Mover a `src/main/resources/db/changelog/`

### ❌ JAR muy grande

**Normal:** 50-100 MB (incluye todas las dependencias)

**Si es >200 MB:** Verifica que no haya archivos extra en `src/main/resources`

---

## 🧪 Testing del JAR

### Script de Prueba (PowerShell)

```powershell
# test-jar.ps1

Write-Host "=== Verificando JAR ===" -ForegroundColor Cyan

# 1. Verificar que existe
if (Test-Path "target/store-0.0.1-SNAPSHOT.jar") {
    Write-Host "✓ JAR encontrado" -ForegroundColor Green
} else {
    Write-Host "✗ JAR no encontrado" -ForegroundColor Red
    exit 1
}

# 2. Verificar migraciones
Write-Host "`nVerificando migraciones en el JAR..." -ForegroundColor Yellow
jar -tf target/store-0.0.1-SNAPSHOT.jar | Select-String "db/changelog"

# 3. Ejecutar JAR
Write-Host "`nIniciando JAR..." -ForegroundColor Yellow
java -jar target/store-0.0.1-SNAPSHOT.jar
```

---

## 📝 Resumen

### ✅ Lo que SÍ se incluye automáticamente:

- ✅ Archivos de migración YAML
- ✅ application.properties
- ✅ Archivos estáticos (HTML, CSS, JS)
- ✅ Clases compiladas (.class)
- ✅ Dependencias (JARs)

### ❌ Lo que NO se incluye:

- ❌ Archivo `.env` (variables de entorno)
- ❌ Archivos de `src/test/`
- ❌ Archivos de documentación (README, etc.)
- ❌ Archivos de configuración del IDE

### 🎯 Comando Completo para Deployment

```bash
# 1. Construir
mvn clean package -DskipTests

# 2. Verificar migraciones
jar -tf target/store-0.0.1-SNAPSHOT.jar | Select-String "changelog"

# 3. Ejecutar
java -jar target/store-0.0.1-SNAPSHOT.jar
```

---

## 📚 Referencias

- [Spring Boot - Executable JAR](https://docs.spring.io/spring-boot/docs/current/reference/html/executable-jar.html)
- [Liquibase - Running with Java](https://docs.liquibase.com/tools-integrations/springboot/using-springboot-with-maven.html)
- [Maven - Building JARs](https://maven.apache.org/plugins/maven-jar-plugin/)

---

## ✨ Conclusión

**Respuesta:** Sí, las migraciones se incluyen automáticamente en el JAR.

**No necesitas:**
- ❌ Copiar archivos manualmente
- ❌ Empaquetar migraciones por separado
- ❌ Configuración adicional de Maven

**Solo necesitas:**
- ✅ Ejecutar `mvn package`
- ✅ El JAR tiene todo lo necesario
- ✅ Configurar variables de entorno en el servidor

**¡El JAR es autocontenido y portable! 🚀**

---

**Última actualización:** 2025-11-23

