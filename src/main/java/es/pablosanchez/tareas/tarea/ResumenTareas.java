package es.pablosanchez.tareas.tarea;

/**
 * Recuento de tareas para el panel de resumen.
 *
 * @param total       todas las tareas
 * @param pendientes  tareas sin completar
 * @param completadas tareas completadas
 * @param vencidas    tareas pendientes con la fecha límite pasada
 */
public record ResumenTareas(long total, long pendientes, long completadas, long vencidas) {
}
