package es.pablosanchez.tareas.web;

import es.pablosanchez.tareas.tarea.TareaNoEncontradaException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Convierte las excepciones en respuestas de error con formato estándar
 * <a href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457 (Problem Details)</a>.
 *
 * <pre>
 * {
 *   "title": "Datos no válidos",
 *   "status": 400,
 *   "detail": "La petición contiene errores de validación",
 *   "errores": { "titulo": "El título es obligatorio" }
 * }
 * </pre>
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(TareaNoEncontradaException.class)
    ProblemDetail noEncontrada(TareaNoEncontradaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problema.setTitle("Tarea no encontrada");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "La petición contiene errores de validación");
        problema.setTitle("Datos no válidos");
        problema.setProperty("errores", errores);
        return problema;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail formatoIncorrecto(Exception e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Revisa el formato: fechas AAAA-MM-DD y prioridad BAJA, MEDIA o ALTA");
        problema.setTitle("Petición mal formada");
        return problema;
    }
}
