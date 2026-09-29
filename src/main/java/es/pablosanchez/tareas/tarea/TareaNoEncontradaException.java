package es.pablosanchez.tareas.tarea;

/**
 * Se lanza cuando se pide una tarea que no existe. La API responde con 404.
 */
public class TareaNoEncontradaException extends RuntimeException {

    /**
     * Crea la excepción para el identificador indicado.
     *
     * @param id identificador buscado
     */
    public TareaNoEncontradaException(Long id) {
        super("No existe ninguna tarea con id " + id);
    }
}
