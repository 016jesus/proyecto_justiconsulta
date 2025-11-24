# ===================================
# Script de Verificación Pre-Ejecución
# ===================================
# Este script verifica que todo esté configurado correctamente

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  VERIFICACIÓN DE CONFIGURACIÓN" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$errores = 0

# 1. Verificar archivo .env
Write-Host "[1/5] Verificando archivo .env..." -ForegroundColor Yellow
if (Test-Path ".env") {
    Write-Host "  ✓ Archivo .env existe" -ForegroundColor Green

    # Verificar variables importantes
    $envContent = Get-Content .env -Raw
    $variablesRequeridas = @(
        "JDBC_DATABASE_URL",
        "SUPABASE_USER",
        "SUPABASE_PASS",
        "JWT_SECRET_KEY",
        "API_SECRET_KEY"
    )

    foreach ($var in $variablesRequeridas) {
        if ($envContent -match $var) {
            Write-Host "  ✓ Variable $var encontrada" -ForegroundColor Green
        } else {
            Write-Host "  ✗ Variable $var NO encontrada" -ForegroundColor Red
            $errores++
        }
    }
} else {
    Write-Host "  ✗ Archivo .env NO existe" -ForegroundColor Red
    Write-Host "    Copia .env.example a .env y configura las variables" -ForegroundColor Yellow
    $errores++
}
Write-Host ""

# 2. Verificar Java
Write-Host "[2/5] Verificando Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "  ✓ Java instalado: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Java NO encontrado" -ForegroundColor Red
    $errores++
}
Write-Host ""

# 3. Verificar Maven
Write-Host "[3/5] Verificando Maven..." -ForegroundColor Yellow
try {
    $mavenVersion = mvn -version 2>&1 | Select-String "Apache Maven"
    Write-Host "  ✓ Maven instalado: $mavenVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Maven NO encontrado" -ForegroundColor Red
    $errores++
}
Write-Host ""

# 4. Verificar application.properties
Write-Host "[4/4] Verificando application.properties..." -ForegroundColor Yellow
if (Test-Path "src\main\resources\application.properties") {
    $appProps = Get-Content "src\main\resources\application.properties" -Raw

    if ($appProps -match "spring.jpa.hibernate.ddl-auto=none") {
        Write-Host "  ✓ Hibernate DDL configurado correctamente" -ForegroundColor Green
    } else {
        Write-Host "  ! Advertencia: Hibernate DDL no está en 'none'" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ✗ application.properties NO existe" -ForegroundColor Red
    $errores++
}
Write-Host ""

# Resumen
Write-Host "========================================" -ForegroundColor Cyan
if ($errores -eq 0) {
    Write-Host "  ✓ TODO CORRECTO" -ForegroundColor Green
    Write-Host "  Puedes ejecutar la aplicación con:" -ForegroundColor White
    Write-Host "    mvn spring-boot:run" -ForegroundColor Cyan
} else {
    Write-Host "  ✗ SE ENCONTRARON $errores ERROR(ES)" -ForegroundColor Red
    Write-Host "  Por favor corrige los errores antes de ejecutar" -ForegroundColor Yellow
}
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Documentación
Write-Host "📚 DOCUMENTACIÓN DISPONIBLE:" -ForegroundColor Cyan
Write-Host "  - README.md                      Documentación general" -ForegroundColor White
Write-Host "  - ENV_SETUP.md                   Configuración de variables de entorno en IntelliJ" -ForegroundColor White
Write-Host "  - GMAIL_SMTP_SETUP.md            Configuración de SMTP para Gmail" -ForegroundColor White
Write-Host ""

