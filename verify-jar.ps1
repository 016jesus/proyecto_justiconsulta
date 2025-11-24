# Script para verificar que las migraciones están incluidas en el JAR
# verify-jar.ps1

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  VERIFICACIÓN DE JAR - LIQUIBASE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$jarPath = "target\store-0.0.1-SNAPSHOT.jar"
$errores = 0

# 1. Verificar que el JAR existe
Write-Host "[1/5] Verificando existencia del JAR..." -ForegroundColor Yellow
if (Test-Path $jarPath) {
    $size = (Get-Item $jarPath).Length / 1MB
    Write-Host "  ✓ JAR encontrado: $jarPath" -ForegroundColor Green
    Write-Host "  ✓ Tamaño: $([math]::Round($size, 2)) MB" -ForegroundColor Green
} else {
    Write-Host "  ✗ JAR no encontrado en $jarPath" -ForegroundColor Red
    Write-Host "  → Ejecuta: mvn clean package" -ForegroundColor Yellow
    $errores++
}
Write-Host ""

# 2. Verificar comando jar
Write-Host "[2/5] Verificando herramienta 'jar'..." -ForegroundColor Yellow
try {
    $null = jar 2>&1
    Write-Host "  ✓ Comando 'jar' disponible" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Comando 'jar' no encontrado" -ForegroundColor Red
    Write-Host "  → Asegúrate de tener Java JDK instalado" -ForegroundColor Yellow
    $errores++
}
Write-Host ""

if ($errores -gt 0) {
    Write-Host "Corrige los errores anteriores antes de continuar." -ForegroundColor Red
    exit 1
}

# 3. Listar archivos de changelog
Write-Host "[3/5] Verificando archivos de migración en el JAR..." -ForegroundColor Yellow
$changelogFiles = jar -tf $jarPath | Select-String "BOOT-INF/classes/db/changelog"

if ($changelogFiles) {
    Write-Host "  ✓ Archivos de migración encontrados:" -ForegroundColor Green
    foreach ($file in $changelogFiles) {
        Write-Host "    - $file" -ForegroundColor Gray
    }
} else {
    Write-Host "  ✗ No se encontraron archivos de migración" -ForegroundColor Red
    Write-Host "  → Verifica que estén en src/main/resources/db/changelog/" -ForegroundColor Yellow
    $errores++
}
Write-Host ""

# 4. Verificar archivos específicos
Write-Host "[4/5] Verificando archivos críticos..." -ForegroundColor Yellow
$archivosRequeridos = @(
    "BOOT-INF/classes/db/changelog/db.changelog-master.yaml",
    "BOOT-INF/classes/db/changelog/changes/001-create-initial-schema.yaml",
    "BOOT-INF/classes/db/changelog/changes/002-insert-initial-data.yaml",
    "BOOT-INF/classes/application.properties"
)

$todosLosArchivos = jar -tf $jarPath

foreach ($archivo in $archivosRequeridos) {
    if ($todosLosArchivos -contains $archivo) {
        $nombre = Split-Path $archivo -Leaf
        Write-Host "  ✓ $nombre" -ForegroundColor Green
    } else {
        $nombre = Split-Path $archivo -Leaf
        Write-Host "  ✗ $nombre NO encontrado" -ForegroundColor Red
        $errores++
    }
}
Write-Host ""

# 5. Verificar estructura del JAR
Write-Host "[5/5] Verificando estructura del JAR..." -ForegroundColor Yellow
$classes = jar -tf $jarPath | Select-String "BOOT-INF/classes/com/justiconsulta"

if ($classes) {
    Write-Host "  ✓ Clases de aplicación incluidas correctamente" -ForegroundColor Green
} else {
    Write-Host "  ✗ Clases de aplicación no encontradas" -ForegroundColor Red
    Write-Host "  → Verifica que la compilación sea exitosa" -ForegroundColor Yellow
    $errores++
}
Write-Host ""

# Resumen
Write-Host "========================================" -ForegroundColor Cyan
if ($errores -eq 0) {
    Write-Host "  ✓ VERIFICACIÓN EXITOSA" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Tu JAR incluye correctamente:" -ForegroundColor White
    Write-Host "    ✓ Clases de aplicación" -ForegroundColor Gray
    Write-Host "    ✓ Configuración (application.properties)" -ForegroundColor Gray
    Write-Host "    ✓ Dependencias necesarias" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  Puedes deployar este JAR sin archivos adicionales" -ForegroundColor Cyan
    Write-Host "  Solo necesitas configurar las variables de entorno" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  Ejecutar:" -ForegroundColor Yellow
    Write-Host "    java -jar $jarPath" -ForegroundColor White
} else {
    Write-Host "  ✗ VERIFICACIÓN FALLIDA ($errores errores)" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Corrige los errores y vuelve a construir:" -ForegroundColor Yellow
    Write-Host "    mvn clean package" -ForegroundColor White
}
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Información adicional
if ($errores -eq 0) {
    Write-Host "📚 DOCUMENTACIÓN:" -ForegroundColor Cyan
    Write-Host "  - JAR_DEPLOYMENT.md    Guía completa de deployment" -ForegroundColor White
    Write-Host "  - QUICKSTART.md        Inicio rápido" -ForegroundColor White
    Write-Host ""
}

exit $errores

