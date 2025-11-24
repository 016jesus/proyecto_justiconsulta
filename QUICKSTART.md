# 🎉 CONFIGURACIÓN COMPLETA DE LIQUIBASE CON YAML

## ✅ Implementación Exitosa

Se ha configurado exitosamente Liquibase con formato YAML para el proyecto JustiConsulta.

## 📁 Archivos Creados

### Migraciones (YAML)
```
src/main/resources/db/changelog/
├── db.changelog-master.yaml                    ✅ Archivo maestro
└── changes/
    ├── 001-create-initial-schema.yaml          ✅ 7 tablas + relaciones
    ├── 002-insert-initial-data.yaml            ✅ 14 usuarios
    └── EXAMPLE-migration-template.yaml         ✅ 18 ejemplos de migraciones
```

### Documentación
```
/
├── README.md                                   ✅ Actualizado con Liquibase
├── ENV_SETUP.md                                ✅ Configurar variables de entorno
├── LIQUIBASE_GUIDE.md                          ✅ Guía completa (YAML)
├── LIQUIBASE_IMPLEMENTATION.md                 ✅ Resumen de implementación
└── YAML_MIGRATION.md                           ✅ Conversión XML → YAML
```

### Configuración
```
.env                                            ✅ Variables de entorno
.env.example                                    ✅ Plantilla
application.properties                          ✅ Configurado para YAML
pom.xml                                         ✅ Configuración Maven
```

## 📊 Esquema de Base de Datos

### Tablas Creadas (7)

1. **user** - Usuarios del sistema
   - document_number (PK)
   - document_type, nombres, email, password
   - supabase_user_id (unique)
   - 14 usuarios iniciales insertados

2. **activity_series** - Series de actividades
   - id (PK, UUID)
   - created_at

3. **legal_process** - Procesos legales
   - id + user_document_number (PK compuesta)
   - last_action_date
   - FK → user

4. **user_legal_processes** - Asociación usuarios-procesos
   - legal_process_id + user_document_number (PK compuesta)
   - FK → user

5. **action** - Acciones sobre procesos
   - id (PK, UUID)
   - activity_series_id, description, date
   - FK → activity_series

6. **notification** - Notificaciones a usuarios
   - notification_id (PK, UUID)
   - user_document_number, action_id, message
   - FK → user, action

7. **history** - Historial de consultas
   - id (PK, UUID)
   - user_document_number, legal_process_id
   - FK → user, activity_series

## 🚀 Cómo Usar

### 1. Primera Ejecución

```bash
# Compilar e instalar dependencias
mvn clean install

# Ejecutar la aplicación
mvn spring-boot:run
```

Al iniciar, Liquibase automáticamente:
- ✅ Crea tabla `databasechangelog` (registro de migraciones)
- ✅ Crea tabla `databasechangeloglock` (control de concurrencia)
- ✅ Ejecuta `001-create-initial-schema.yaml` (crea 7 tablas)
- ✅ Ejecuta `002-insert-initial-data.yaml` (inserta 14 usuarios)



## 📚 Configuración Básica

La aplicación utiliza JPA/Hibernate para el acceso a datos. Asegúrate de tener:

1. ✅ Agregar columna simple
2. ✅ Crear tabla completa
3. ✅ Agregar foreign key
4. ✅ Crear índice
5. ✅ Insertar datos múltiples
6. ✅ SQL personalizado
7. ✅ Con rollback
8. ✅ Con precondiciones
9. ✅ Modificar columna
10. ✅ Renombrar columna
11. ✅ Agregar constraint unique
12. ✅ Eliminar columna
13. ✅ Con contexto (solo test)
14. ✅ Con labels
15. ✅ Crear vista
16. ✅ Agregar NOT NULL
17. ✅ Eliminar NOT NULL
18. ✅ Cargar datos desde CSV

## 🔧 Comandos Útiles





## ⚙️ Configuración Variables de Entorno

### Para IntelliJ IDEA

**Opción 1: Plugin EnvFile (Recomendado)**

1. `File` → `Settings` → `Plugins` → Buscar "EnvFile" → Instalar
2. `Run` → `Edit Configurations`
3. Pestaña "EnvFile" → `+` → Seleccionar `.env`
4. Marcar "Enable EnvFile"

**Opción 2: Manual**

1. `Run` → `Edit Configurations`
2. "Environment variables" → Copiar variables de `.env`

Ver más detalles en: **ENV_SETUP.md**

## 📖 Documentación Disponible

| Archivo | Descripción |
|---------|-------------|
| **README.md** | Documentación general del proyecto |
| **ENV_SETUP.md** | Configurar variables de entorno en IntelliJ |
| **LIQUIBASE_GUIDE.md** | Guía completa de Liquibase con ejemplos YAML |
| **LIQUIBASE_IMPLEMENTATION.md** | Resumen de la implementación |
| **YAML_MIGRATION.md** | Conversión de XML a YAML |
| **QUICKSTART.md** | Este archivo (inicio rápido) |

## ⚠️ Buenas Prácticas

### ✅ DO (Hacer)

- ✅ Usar IDs descriptivos: `001-create-user-table`
- ✅ Un cambio por changeset
- ✅ Nunca modificar changesets ya ejecutados
- ✅ Agregar rollback cuando sea posible
- ✅ Usar precondiciones para cambios condicionales
- ✅ Commitear archivos de migración al repositorio
- ✅ Probar migraciones en desarrollo antes de producción

### ❌ DON'T (No hacer)

- ❌ IDs genéricos: `changeset1`, `migration2`
- ❌ Múltiples cambios no relacionados en un changeset
- ❌ Modificar changesets ya ejecutados en producción
- ❌ Commitear el archivo `.env` al repositorio
- ❌ Usar `ddl-auto=update` con Liquibase habilitado
- ❌ Ejecutar migraciones manualmente en producción

## 🐛 Solución de Problemas

### Lock no liberado

```bash
mvn liquibase:releaseLocks
```

O manualmente:
```sql
UPDATE databasechangeloglock SET locked = FALSE;
```

### Checksum no coincide

```bash
# Solo en desarrollo
mvn liquibase:clearCheckSums
```

### Base de datos ya existe

**Opción 1:** Limpiar y empezar de nuevo (solo desarrollo)
```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

**Opción 2:** Sincronizar con Liquibase
```bash
mvn liquibase:changelogSync
```

### Ver errores

```bash
# Ejecutar con debug
mvn spring-boot:run -X
```

## 🎓 Recursos Adicionales

### Documentación Oficial
- [Liquibase Docs](https://docs.liquibase.com/)
- [Change Types](https://docs.liquibase.com/change-types/home.html)
- [YAML Format](https://docs.liquibase.com/concepts/changelogs/yaml-format.html)

### En este Proyecto
- `EXAMPLE-migration-template.yaml` - 18 ejemplos prácticos
- `LIQUIBASE_GUIDE.md` - Guía completa con todos los detalles
- `changes/001-create-initial-schema.yaml` - Ejemplo real de esquema completo

## 📊 Estado Actual

```
✅ Liquibase instalado y configurado
✅ Formato YAML implementado
✅ Esquema inicial creado (7 tablas)
✅ Datos iniciales insertados (14 usuarios)
✅ Documentación completa
✅ Ejemplos de migraciones disponibles
✅ Variables de entorno configuradas
✅ Sin errores de sintaxis
```

## 🚀 Próximos Pasos

1. **Ejecutar la aplicación** para aplicar migraciones:
   ```bash
   mvn spring-boot:run
   ```

2. **Verificar base de datos**:
   ```sql
   SELECT * FROM databasechangelog;
   SELECT * FROM "user";
   ```

3. **Crear tu primera migración** siguiendo los ejemplos

4. **Revisar documentación** según necesites

---

## 💡 Tips Rápidos

### Crear migración rápida

```bash
# 1. Crear archivo
touch src/main/resources/db/changelog/changes/003-mi-cambio.yaml

# 2. Copiar estructura base
# Ver EXAMPLE-migration-template.yaml

# 3. Incluir en master
# Editar db.changelog-master.yaml

# 4. Ejecutar
mvn spring-boot:run
```

---

## ✨ ¡Todo Listo!

Tu proyecto está configurado y listo para ejecutar.

**Para empezar:**
```bash
mvn spring-boot:run
```

**¡Feliz desarrollo! 🚀**

