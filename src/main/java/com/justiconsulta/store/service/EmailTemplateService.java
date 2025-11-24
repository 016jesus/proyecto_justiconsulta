package com.justiconsulta.store.service;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    /**
     * Plantilla base HTML para todos los correos
     */
    private String getBaseTemplate(String content, String title) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    :root{
                        --navy: #0F172A;
                        --amber: #D97706;
                        --muted: #6b7280;
                        --bg: #f4f7fa;
                        --card: #ffffff;
                    }
                    body {
                        margin: 0;
                        padding: 0;
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background-color: var(--bg);
                    }
                    .email-container {
                        max-width: 680px;
                        margin: 24px auto;
                        background-color: var(--card);
                        border-radius: 12px;
                        box-shadow: 0 8px 24px rgba(15,23,42,0.08);
                        overflow: hidden;
                    }
                    .header {
                        background: linear-gradient(180deg, var(--navy) 0%%, rgba(15,23,42,0.95) 100%%);
                        padding: 36px 20px;
                        text-align: center;
                    }
                    .header h1 {
                        color: #ffffff;
                        margin: 0;
                        font-size: 26px;
                        font-weight: 700;
                        letter-spacing: 0.4px;
                    }
                    .header .logo {
                        font-size: 40px;
                        margin-bottom: 8px;
                    }
                    .content {
                        padding: 32px 36px;
                        color: #111827;
                        line-height: 1.6;
                    }
                    .content h2 {
                        color: var(--navy);
                        margin-top: 0;
                        font-size: 20px;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 28px;
                        background: var(--amber);
                        color: #0f172a !important;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 700;
                        margin: 20px 0;
                        transition: transform 0.12s ease-in-out, box-shadow 0.12s;
                    }
                    .button:hover {
                        transform: translateY(-3px);
                        box-shadow: 0 8px 20px rgba(217,119,6,0.18);
                    }
                    .info-box {
                        background-color: #fff7ed;
                        border-left: 4px solid var(--amber);
                        padding: 14px 18px;
                        margin: 18px 0;
                        border-radius: 6px;
                    }
                    .footer {
                        background-color: var(--navy);
                        color: #ffffff;
                        padding: 22px 30px;
                        text-align: center;
                        font-size: 13px;
                    }
                    .footer a {
                        color: var(--amber);
                        text-decoration: none;
                    }
                    .divider {
                        height: 1px;
                        background-color: #e6e7ea;
                        margin: 26px 0;
                    }
                    .highlight {
                        color: var(--amber);
                        font-weight: 700;
                    }
                    ul { padding-left: 18px; }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <div class="logo">⚖️</div>
                        <h1>JustiConsulta</h1>
                    </div>
                    <div class="content">
                        %s
                    </div>
                    <div class="footer">
                        <p><strong>JustiConsulta</strong></p>
                        <p>Tu plataforma de consulta de procesos judiciales</p>
                        <p style="margin-top: 8px; font-size: 12px; color: rgba(255,255,255,0.85);">
                            Este es un correo automático, por favor no respondas a este mensaje.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(title, content);
    }

    /**
     * Plantilla de bienvenida para nuevos usuarios
     */
    public String getWelcomeEmailTemplate(String userName, String userEmail) {
        String content = """
            <h2>¡Bienvenido a JustiConsulta! 🎉</h2>
            <p>Hola <span class="highlight">%s</span>,</p>
            <p>Nos complace darte la bienvenida a <strong>JustiConsulta</strong>, tu plataforma confiable para consultar y realizar seguimiento de procesos judiciales en Colombia.</p>
            
            <div class="info-box">
                <p style="margin: 0;"><strong>📧 Tu cuenta:</strong> %s</p>
            </div>
            
            <p>Con JustiConsulta podrás:</p>
            <ul>
                <li>✅ Consultar procesos judiciales por número de radicación</li>
                <li>✅ Realizar seguimiento de actuaciones procesales</li>
                <li>✅ Recibir notificaciones sobre nuevas actuaciones</li>
                <li>✅ Acceder al historial completo de tus consultas</li>
            </ul>
            
            <div style="text-align: center;">
                <a href="${APP_URL}" class="button">Iniciar Sesión</a>
            </div>
            
            <div class="divider"></div>
            
            <p style="font-size: 14px; color: var(--muted);">
                Si tienes alguna pregunta o necesitas ayuda, no dudes en contactarnos. 
                Estamos aquí para ayudarte.
            </p>
            """.formatted(userName, userEmail);

        return getBaseTemplate(content, "Bienvenido a JustiConsulta");
    }

    /**
     * Plantilla de notificación de nueva actuación procesal
     */
    public String getNewActuationEmailTemplate(String userName, String numeroRadicacion,
                                                String actuacion, String fecha) {
        String content = """
            <h2>Nueva Actuación Procesal 📋</h2>
            <p>Hola <span class="highlight">%s</span>,</p>
            <p>Te notificamos que se ha registrado una nueva actuación en uno de los procesos que estás siguiendo.</p>
            
            <div class="info-box">
                <p style="margin: 5px 0;"><strong>Número de Radicación:</strong> %s</p>
                <p style="margin: 5px 0;"><strong>Fecha:</strong> %s</p>
                <p style="margin: 5px 0;"><strong>Actuación:</strong></p>
                <p style="margin: 5px 0; font-size: 14px;">%s</p>
            </div>
            
            <div style="text-align: center;">
                <a href="${APP_URL}/legal-processes/%s" class="button">Ver Detalles del Proceso</a>
            </div>
            
            <p style="font-size: 14px; color: var(--muted); margin-top: 30px;">
                💡 <em>Consejo:</em> Mantente al tanto de todas las actuaciones de tus procesos 
                ingresando regularmente a la plataforma.
            </p>
            """.formatted(userName, numeroRadicacion, fecha, actuacion, numeroRadicacion);

        return getBaseTemplate(content, "Nueva Actuación Procesal");
    }

    /**
     * Plantilla de recordatorio de actuaciones pendientes
     */
    public String getActuationReminderEmailTemplate(String userName, int cantidadProcesos) {
        String content = """
            <h2>Recordatorio de Actuaciones ⏰</h2>
            <p>Hola <span class="highlight">%s</span>,</p>
            <p>Este es un recordatorio para que revises el estado de tus procesos judiciales.</p>
            
            <div class="info-box">
                <p style="margin: 0;">Actualmente tienes <strong class="highlight">%d proceso(s)</strong> en seguimiento.</p>
            </div>
            
            <p>Te recomendamos revisar regularmente el estado de tus procesos para estar al tanto de cualquier novedad.</p>
            
            <div style="text-align: center;">
                <a href="${APP_URL}/my-processes" class="button">Ver Mis Procesos</a>
            </div>
            
            <p style="font-size: 14px; color: var(--muted); margin-top: 30px;">
                📌 Este es un mensaje automático de recordatorio. Puedes configurar la frecuencia 
                de estos recordatorios en tu perfil.
            </p>
            """.formatted(userName, cantidadProcesos);

        return getBaseTemplate(content, "Recordatorio de Actuaciones");
    }

    /**
     * Plantilla de recuperación de contraseña
     */
    public String getPasswordResetEmailTemplate(String userName, String resetLink) {
        String content = """
            <h2>Recuperación de Contraseña 🔐</h2>
            <p>Hola <span class="highlight">%s</span>,</p>
            <p>Hemos recibido una solicitud para restablecer tu contraseña en JustiConsulta.</p>
            
            <div class="info-box">
                <p style="margin: 0;">
                    ⚠️ Si no solicitaste este cambio, puedes ignorar este correo de forma segura.
                </p>
            </div>
            
            <p>Para restablecer tu contraseña, haz clic en el siguiente botón:</p>
            
            <div style="text-align: center;">
                <a href="%s" class="button">Restablecer Contraseña</a>
            </div>
            
            <p style="font-size: 14px; color: var(--muted); margin-top: 30px;">
                🕐 Este enlace expirará en 1 hora por razones de seguridad.
            </p>
            
            <p style="font-size: 13px; color: #9ca3af; margin-top: 20px;">
                Si el botón no funciona, copia y pega el siguiente enlace en tu navegador:<br>
                <a href="%s" style="color: var(--amber); word-break: break-all;">%s</a>
            </p>
            """.formatted(userName, resetLink, resetLink, resetLink);

        return getBaseTemplate(content, "Recuperación de Contraseña");
    }

    /**
     * Plantilla de confirmación de eliminación de proceso
     */
    public String getProcessDeletedEmailTemplate(String userName, String numeroRadicacion) {
        String content = """
            <h2>Proceso Eliminado del Seguimiento ✓</h2>
            <p>Hola <span class="highlight">%s</span>,</p>
            <p>Te confirmamos que el siguiente proceso ha sido eliminado de tu lista de seguimiento:</p>
            
            <div class="info-box">
                <p style="margin: 0;"><strong>Número de Radicación:</strong> %s</p>
            </div>
            
            <p>Ya no recibirás notificaciones sobre actuaciones de este proceso.</p>
            
            <div style="text-align: center;">
                <a href="${APP_URL}/my-processes" class="button">Ver Mis Procesos Activos</a>
            </div>
            
            <p style="font-size: 14px; color: var(--muted); margin-top: 30px;">
                Si eliminaste este proceso por error, puedes volver a agregarlo en cualquier momento 
                realizando una nueva búsqueda.
            </p>
            """.formatted(userName, numeroRadicacion);

        return getBaseTemplate(content, "Proceso Eliminado del Seguimiento");
    }

    /**
     * Plantilla de notificación de múltiples actuaciones
     */
    public String getMultipleActuationsEmailTemplate(String userName, String actuacionesHtml) {
        String content = """
            <h2>Resumen de Actuaciones Recientes 📊</h2>
            <p>Hola <span class="highlight">%s</span>,</p>
            <p>Te enviamos un resumen de las actuaciones recientes en tus procesos:</p>
            
            %s
            
            <div style="text-align: center;">
                <a href="${APP_URL}/my-processes" class="button">Ver Todos Mis Procesos</a>
            </div>
            
            <p style="font-size: 14px; color: var(--muted); margin-top: 30px;">
                💼 Mantente informado sobre el estado de tus procesos judiciales.
            </p>
            """.formatted(userName, actuacionesHtml);

        return getBaseTemplate(content, "Resumen de Actuaciones");
    }

    /**
     * Plantilla de notificación de proceso asociado exitosamente
     */
    public String getProcessAssociatedEmailTemplate(String userName, String numeroRadicacion, String despacho) {
        String content = """
            <h2>Proceso Agregado a tu Seguimiento ✅</h2>
            <p>Hola <span class="highlight">%s</span>,</p>
            <p>Has agregado exitosamente un nuevo proceso judicial a tu lista de seguimiento en JustiConsulta.</p>
            
            <div class="info-box">
                <p style="margin: 5px 0;"><strong>Número de Radicación:</strong> %s</p>
                <p style="margin: 5px 0;"><strong>Despacho:</strong> %s</p>
            </div>
            
            <p>A partir de ahora recibirás notificaciones cada vez que haya una nueva actuación en este proceso.</p>
            
            <h3 style="color: var(--navy); font-size: 18px; margin-top: 30px;">¿Qué puedes hacer ahora?</h3>
            <ul style="line-height: 1.8;">
                <li>📋 Consultar el detalle completo del proceso</li>
                <li>📝 Ver todas las actuaciones procesales</li>
                <li>👥 Revisar los sujetos procesales involucrados</li>
                <li>📄 Acceder a los documentos disponibles</li>
            </ul>
            
            <div style="text-align: center;">
                <a href="${APP_URL}/legal-processes/%s" class="button">Ver Detalle del Proceso</a>
            </div>
            
            <div class="divider"></div>
            
            <p style="font-size: 14px; color: var(--muted);">
                💡 <em>Tip:</em> Puedes gestionar todos tus procesos en seguimiento desde tu panel de control.
            </p>
            """.formatted(userName, numeroRadicacion, despacho != null ? despacho : "No disponible", numeroRadicacion);

        return getBaseTemplate(content, "Proceso Agregado a tu Seguimiento");
    }
}
