package es.pablosanchez.tareas.tarea;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Datos que envía el cliente para crear o modificar una tarea.
 *
 * @param titulo      título obligatorio
 * @param descripcion descripción opcional
 * @param prioridad   prioridad; si se omite, {@code MEDIA}
 * @param fechaLimite fecha límite opcional en formato {@code AAAA-MM-DD}
 */
public record TareaRequest(
        @Schema(example = "Entregar la práctica de DWES")
        @NotBlank(message = "El título es obligatorio")
        @Size(max = MAX_TITULO, message = "El título no puede superar {max} caracteres")
        String titulo,

        @Schema(example = "Subir el repositorio y el PDF al aula virtual")
        @Size(max = MAX_DESCRIPCION, message = "La descripción no puede superar {max} caracteres")
        String descripcion,

        @Schema(example = "ALTA")
        Prioridad prioridad,

        @Schema(example = "2026-10-15")
        LocalDate fechaLimite) {

    /** Longitud máxima del título. */
    public static final int MAX_TITULO = 120;

    /** Longitud máxima de la descripción. */
    public static final int MAX_DESCRIPCION = 1000;
}
