package com.justiconsulta.store.exception;

import com.justiconsulta.store.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 400 BAD REQUEST - Argumento ilegal
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(
            IllegalArgumentException exception, HttpServletRequest request) {
        log.warn("IllegalArgumentException en {}: {}", request.getRequestURI(), exception.getMessage());
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(exception.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 400 BAD REQUEST - Error de validación de argumentos
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        log.warn("Errores de validación en {}", request.getRequestURI());
        
        Map<String, String> fieldErrors = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("message", "Errores de validación en los campos");
        response.put("path", request.getRequestURI());
        response.put("errors", fieldErrors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 400 BAD REQUEST - Tipo de argumento incorrecto
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        log.warn("MethodArgumentTypeMismatchException en {}: {}", request.getRequestURI(), exception.getMessage());
        
        String message = String.format("El parámetro '%s' debe ser de tipo %s", 
                exception.getName(), 
                exception.getRequiredType() != null ? exception.getRequiredType().getSimpleName() : "desconocido");

        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST, 
                message, 
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 400 BAD REQUEST - JSON mal formado
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        log.warn("HttpMessageNotReadableException en {}: {}", request.getRequestURI(), exception.getMessage());
        
        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST, 
                "El cuerpo de la solicitud no es válido o está mal formado", 
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 400 BAD REQUEST - Parámetro requerido faltante
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        log.warn("MissingServletRequestParameterException en {}: {}", request.getRequestURI(), exception.getMessage());
        
        String message = String.format("Falta el parámetro requerido '%s' de tipo %s", 
                exception.getParameterName(), 
                exception.getParameterType());

        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST, 
                message, 
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 401 UNAUTHORIZED - Credenciales incorrectas
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException exception, HttpServletRequest request) {
        log.warn("BadCredentialsException en {}", request.getRequestURI());
        
        Map<String, Object> response = createErrorResponse(
                HttpStatus.UNAUTHORIZED, 
                "Credenciales incorrectas. Verifica tu email y contraseña", 
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // 401 UNAUTHORIZED - Error de autenticación general
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException exception, HttpServletRequest request) {
        log.warn("AuthenticationException en {}: {}", request.getRequestURI(), exception.getMessage());
        
        Map<String, Object> response = createErrorResponse(
                HttpStatus.UNAUTHORIZED, 
                "Error de autenticación. Por favor, inicia sesión nuevamente", 
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // 403 FORBIDDEN - Acceso denegado
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        log.warn("AccessDeniedException en {} para usuario: {}", 
                request.getRequestURI(), 
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anónimo");
        
        Map<String, Object> response = createErrorResponse(
                HttpStatus.FORBIDDEN, 
                "No tienes permisos para acceder a este recurso", 
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    // 404 NOT FOUND - Recurso no encontrado
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(
            ResourceNotFoundException exception, HttpServletRequest request) {
        log.warn("ResourceNotFoundException en {}: {}", request.getRequestURI(), exception.getMessage());
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(exception.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // 404 NOT FOUND - Endpoint no encontrado
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(
            NoHandlerFoundException exception, HttpServletRequest request) {
        log.warn("NoHandlerFoundException: {} {}", exception.getHttpMethod(), exception.getRequestURL());
        
        Map<String, Object> response = createErrorResponse(
                HttpStatus.NOT_FOUND, 
                String.format("El endpoint %s %s no existe", exception.getHttpMethod(), exception.getRequestURL()), 
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // 404 NOT FOUND - Recurso estático no encontrado
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(
            NoResourceFoundException exception, HttpServletRequest request) {
        log.warn("NoResourceFoundException: {}", exception.getResourcePath());
        
        Map<String, Object> response = createErrorResponse(
                HttpStatus.NOT_FOUND, 
                String.format("El recurso '%s' no fue encontrado", exception.getResourcePath()), 
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // 405 METHOD NOT ALLOWED - Método HTTP no permitido
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        log.warn("HttpRequestMethodNotSupportedException en {}: método {} no soportado", 
                request.getRequestURI(), exception.getMethod());
        
        String message = String.format("El método %s no está permitido para esta ruta. Métodos soportados: %s", 
                exception.getMethod(), 
                exception.getSupportedHttpMethods());

        Map<String, Object> response = createErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED, 
                message, 
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // 415 UNSUPPORTED MEDIA TYPE - Tipo de contenido no soportado
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        log.warn("HttpMediaTypeNotSupportedException en {}: {}", request.getRequestURI(), exception.getMessage());
        
        String message = String.format("El tipo de contenido '%s' no es soportado. Tipos soportados: %s", 
                exception.getContentType(), 
                exception.getSupportedMediaTypes());

        Map<String, Object> response = createErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, 
                message, 
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // 422 UNPROCESSABLE ENTITY - Estado ilegal
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException exception, HttpServletRequest request) {
        log.warn("IllegalStateException en {}: {}", request.getRequestURI(), exception.getMessage());

        Map<String, Object> response = createErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // 500 INTERNAL SERVER ERROR - Error general
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception exception, HttpServletRequest request) {
        log.error("Error inesperado en {}: {}", request.getRequestURI(), exception.getMessage(), exception);

        Map<String, Object> response = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno inesperado en el servidor. Por favor, contacta al administrador",
                request.getRequestURI()
        );

        // En desarrollo, puedes incluir el stack trace
        if (log.isDebugEnabled()) {
            response.put("trace", exception.getStackTrace());
        }

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Método auxiliar para crear respuestas de error consistentes
    private Map<String, Object> createErrorResponse(HttpStatus status, String message, String path) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        response.put("path", path);
        return response;
    }
}