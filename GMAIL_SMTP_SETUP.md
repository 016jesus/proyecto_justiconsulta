# Configuración de Notificaciones por Email con Gmail SMTP

## 📧 Sistema de Notificaciones Implementado

Este proyecto incluye un sistema completo de notificaciones por correo electrónico usando Gmail SMTP con plantillas HTML atractivas y profesionales.

## 🎨 Tipos de Notificaciones

El sistema soporta los siguientes tipos de correos:

1. **Correo de Bienvenida** - Cuando un usuario se registra
2. **Nueva Actuación Procesal** - Cuando hay una nueva actuación en un proceso seguido
3. **Proceso Eliminado** - Confirmación de eliminación de seguimiento
4. **Recordatorio de Actuaciones** - Recordatorio periódico sobre procesos en seguimiento
5. **Recuperación de Contraseña** - Para resetear contraseña

## 🔧 Configuración de Gmail SMTP

### Paso 1: Habilitar Verificación en 2 Pasos

1. Ve a tu cuenta de Google: https://myaccount.google.com
2. Selecciona **Seguridad** en el menú lateral
3. En "Cómo inicias sesión en Google", selecciona **Verificación en 2 pasos**
4. Sigue los pasos para habilitar la verificación en 2 pasos

### Paso 2: Generar Contraseña de Aplicación

1. Una vez habilitada la verificación en 2 pasos, ve a: https://myaccount.google.com/apppasswords
2. En "Seleccionar app", elige **Correo**
3. En "Seleccionar dispositivo", elige **Otro (nombre personalizado)**
4. Escribe un nombre como "JustiConsulta" y haz clic en **Generar**
5. Google te mostrará una contraseña de 16 caracteres (sin espacios)
6. **IMPORTANTE**: Copia esta contraseña, no podrás verla de nuevo

### Paso 3: Configurar Variables de Entorno

Edita el archivo `.env` en la raíz del proyecto:

```bash
# Gmail SMTP Configuration
GMAIL_USERNAME=tu-correo@gmail.com
GMAIL_APP_PASSWORD=abcdefghijklmnop  # La contraseña de 16 caracteres generada

# Application URL (para los enlaces en los correos)
APP_URL=http://localhost:8080
```

### Paso 4: Establecer Variables de Entorno en Windows

**Opción A - Temporal (en el terminal actual):**
```cmd
set GMAIL_USERNAME=tu-correo@gmail.com
set GMAIL_APP_PASSWORD=abcdefghijklmnop
set APP_URL=http://localhost:8080
```

**Opción B - Permanente (para el usuario actual):**
```cmd
setx GMAIL_USERNAME "tu-correo@gmail.com"
setx GMAIL_APP_PASSWORD "abcdefghijklmnop"
setx APP_URL "http://localhost:8080"
```

> **Nota**: Después de usar `setx`, cierra y abre una nueva ventana de terminal para que surtan efecto.

## 🚀 Ejecutar la Aplicación

```cmd
.\mvnw.cmd spring-boot:run
```

## 🧪 Probar el Sistema de Notificaciones

### Endpoints de Prueba Disponibles

El proyecto incluye un controlador de prueba en `/api/test/notifications` con los siguientes endpoints:

#### 1. Información del Sistema de Pruebas
```http
GET http://localhost:8080/api/test/notifications/info
```

#### 2. Enviar Correo de Bienvenida
```http
POST http://localhost:8080/api/test/notifications/welcome
Content-Type: application/json

{
  "userDocumentNumber": "123456789"
}
```

#### 3. Enviar Notificación de Nueva Actuación
```http
POST http://localhost:8080/api/test/notifications/new-actuation
Content-Type: application/json

{
  "userDocumentNumber": "123456789",
  "numeroRadicacion": "50001333100120070007600",
  "actuacion": "Se admite la demanda y se ordena correr traslado a la parte demandada",
  "fecha": "2024-11-23"
}
```

#### 4. Enviar Notificación de Proceso Eliminado
```http
POST http://localhost:8080/api/test/notifications/process-deleted
Content-Type: application/json

{
  "userDocumentNumber": "123456789",
  "numeroRadicacion": "50001333100120070007600"
}
```

#### 5. Enviar Recordatorio de Actuaciones
```http
POST http://localhost:8080/api/test/notifications/reminder
Content-Type: application/json

{
  "userDocumentNumber": "123456789",
  "cantidadProcesos": 5
}
```

## 📱 Endpoints de Notificaciones para Usuarios

Estos endpoints requieren autenticación JWT:

### Obtener Mis Notificaciones
```http
GET http://localhost:8080/api/notifications/my-notifications
Authorization: Bearer {tu-jwt-token}
```

### Obtener Notificaciones No Leídas
```http
GET http://localhost:8080/api/notifications/my-notifications/unread
Authorization: Bearer {tu-jwt-token}
```

### Contar Notificaciones No Leídas
```http
GET http://localhost:8080/api/notifications/my-notifications/unread/count
Authorization: Bearer {tu-jwt-token}
```

### Marcar Notificación como Leída
```http
PUT http://localhost:8080/api/notifications/{notification-id}/read
Authorization: Bearer {tu-jwt-token}
```

### Marcar Todas como Leídas
```http
PUT http://localhost:8080/api/notifications/mark-all-read
Authorization: Bearer {tu-jwt-token}
```

### Eliminar Notificación
```http
DELETE http://localhost:8080/api/notifications/{notification-id}
Authorization: Bearer {tu-jwt-token}
```

## 🎨 Características de las Plantillas HTML

Las plantillas de correo incluyen:

- ✅ Diseño responsivo y atractivo
- ✅ Colores profesionales con gradientes
- ✅ Emojis para mejor legibilidad
- ✅ Botones de acción con enlaces
- ✅ Información destacada en cajas
- ✅ Footer con información de la aplicación
- ✅ Compatible con la mayoría de clientes de correo

## 🔍 Verificar que Funciona

1. Asegúrate de tener un usuario en la base de datos
2. Usa Postman o cURL para llamar a los endpoints de prueba
3. Revisa la bandeja de entrada del usuario (y carpeta de spam)
4. Revisa los logs de la aplicación para ver si hay errores

### Logs Esperados
Si todo está bien configurado, verás en los logs:
```
INFO  c.j.store.service.impl.EmailServiceImpl : Welcome email sent successfully to: usuario@example.com
```

Si hay un error de configuración:
```
ERROR c.j.store.service.impl.EmailServiceImpl : Error sending welcome email to usuario@example.com: Authentication failed
```

## ⚠️ Solución de Problemas

### Error: "Authentication failed"
- Verifica que hayas habilitado la verificación en 2 pasos
- Asegúrate de usar la contraseña de aplicación, NO tu contraseña normal de Gmail
- Verifica que las variables de entorno estén correctamente configuradas

### Error: "Could not connect to SMTP host"
- Verifica tu conexión a internet
- Asegúrate de que el puerto 587 no esté bloqueado por un firewall
- Intenta desactivar temporalmente tu antivirus/firewall

### Los correos llegan a spam
- Es normal en las primeras pruebas
- Con el tiempo, Gmail aprenderá que tus correos son legítimos
- Considera configurar SPF, DKIM y DMARC para tu dominio en producción

### No llegan correos
- Revisa los logs de la aplicación
- Verifica que el correo del usuario sea válido
- Revisa la carpeta de spam
- Intenta con otro correo de destino

## 🏗️ Estructura del Código

```
service/
├── EmailService.java                    # Interfaz del servicio de email
├── EmailTemplateService.java           # Generador de plantillas HTML
└── impl/
    ├── EmailServiceImpl.java           # Implementación del servicio de email
    └── NotificationServiceImpl.java    # Servicio de notificaciones (email + DB)

controller/
├── NotificationController.java         # Endpoints de notificaciones para usuarios
└── TestNotificationController.java     # Endpoints de prueba para envío de correos

config/
└── MailConfig.java                     # Configuración de JavaMailSender
```

## 📚 Uso Programático

### Enviar Correo de Bienvenida desde el Código

```java
@Autowired
private NotificationServiceImpl notificationService;

// Al registrar un nuevo usuario
public void registerUser(User user) {
    // ... guardar usuario en base de datos ...
    
    // Enviar correo de bienvenida
    notificationService.sendWelcomeNotification(user);
}
```

### Enviar Notificación de Nueva Actuación

```java
// Cuando se detecta una nueva actuación
notificationService.sendNewActuationNotification(
    user,
    "50001333100120070007600",
    "Se admite la demanda",
    "2024-11-23"
);
```

## 🚀 Producción

Para producción, considera:

1. **No usar Gmail SMTP directamente** - Usa servicios como SendGrid, AWS SES, o Mailgun
2. **Configurar SPF, DKIM, DMARC** - Para evitar que tus correos sean marcados como spam
3. **Rate Limiting** - Gmail tiene límites de envío (500 correos/día para cuentas gratuitas)
4. **Monitoreo** - Implementa monitoreo de tasas de entrega y errores
5. **Templates más sofisticados** - Considera usar Thymeleaf para plantillas más complejas

## 📝 Notas Importantes

- **Seguridad**: Nunca commitees tus contraseñas de aplicación en Git
- **Límites de Gmail**: Máximo 500 destinatarios por día para cuentas gratuitas
- **G Suite/Google Workspace**: Tiene límites más altos (2000 correos/día)
- **Alternativas**: Para producción, considera servicios especializados en envío de correos

## 🤝 Soporte

Si tienes problemas con la configuración, verifica:
1. Los logs de la aplicación
2. Que las variables de entorno estén correctamente configuradas
3. Que la contraseña de aplicación sea la correcta (16 caracteres sin espacios)
4. Que la verificación en 2 pasos esté habilitada en tu cuenta de Gmail

