package es.pablosanchez.tareas.tarea;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Representación de una tarea que devuelve la API.
 *
 * @param id            identificador
 * @param titulo        título
 * @param descripcion   descripción, o {@code null}
 * @param prioridad     prioridad
 * @param fechaLimite   fecha límite, o {@code null}
 * @param completada    si está hecha
 * @param vencida       si está pendiente y su fecha límite ya pasó
 * @param creadaEn      momento de creación
 * @param actualizadaEn momento de la última modificación
 */
public record TareaResponse(
        Long id,
        String titulo,
        String descripcion,
        Prioridad prioridad,
        LocalDate fechaLimite,
        boolean completada,
        boolean vencida,
        Instant creadaEn,
        Instant actualizadaEn) {

    /**
     * Convierte una entidad en su representación pública.
     *
     * @param tarea tarea a convertir
     * @param hoy   fecha de referencia para calcular si está vencida
     * @return la respuesta
     */
    public static TareaResponse de(Tarea tarea, LocalDate hoy) {
        return new TareaResponse(
                tarea.getId(),
                tarea.getTitulo(),
                tarea.getDescripcion(),
                tarea.getPrioridad(),
                tarea.getFechaLimite(),
                tarea.isCompletada(),
                tarea.estaVencida(hoy),
                tarea.getCreadaEn(),
                tarea.getActualizadaEn());
    }
}
