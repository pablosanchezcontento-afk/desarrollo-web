package es.pablosanchez.tareas.tarea;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio de las tareas.
 *
 * <p>Recibe un {@link Clock} para que los tests puedan fijar la fecha de hoy y
 * comprobar qué tareas están vencidas sin depender del día en que se ejecutan.</p>
 */
@Service
@Transactional(readOnly = true)
public class TareaService {

    private final TareaRepository repositorio;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param repositorio acceso a datos
     * @param reloj       reloj del sistema (sustituible en tests)
     */
    public TareaService(TareaRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /**
     * Lista las tareas que cumplen los filtros.
     *
     * @param completada estado, o {@code null} para todas
     * @param prioridad  prioridad, o {@code null} para todas
     * @param texto      texto a buscar, o {@code null}
     * @return las tareas encontradas
     */
    public List<TareaResponse> listar(Boolean completada, Prioridad prioridad, String texto) {
        String busqueda = texto == null || texto.isBlank() ? null : texto.strip().toLowerCase(Locale.ROOT);
        LocalDate hoy = hoy();
        return repositorio.buscar(completada, prioridad, busqueda).stream()
                .map(t -> TareaResponse.de(t, hoy))
                .toList();
    }

    /**
     * Devuelve una tarea.
     *
     * @param id identificador
     * @return la tarea
     * @throws TareaNoEncontradaException si no existe
     */
    public TareaResponse obtener(Long id) {
        return TareaResponse.de(buscar(id), hoy());
    }

    /**
     * Crea una tarea pendiente.
     *
     * @param datos datos de la tarea
     * @return la tarea creada, con su identificador
     */
    @Transactional
    public TareaResponse crear(TareaRequest datos) {
        Tarea tarea = new Tarea(datos.titulo(), datos.descripcion(), datos.prioridad(), datos.fechaLimite());
        return TareaResponse.de(repositorio.save(tarea), hoy());
    }

    /**
     * Sustituye los datos editables de una tarea.
     *
     * @param id    identificador
     * @param datos nuevos datos
     * @return la tarea modificada
     * @throws TareaNoEncontradaException si no existe
     */
    @Transactional
    public TareaResponse actualizar(Long id, TareaRequest datos) {
        Tarea tarea = buscar(id);
        tarea.actualizar(datos.titulo(), datos.descripcion(), datos.prioridad(), datos.fechaLimite());
        return TareaResponse.de(repositorio.saveAndFlush(tarea), hoy());
    }

    /**
     * Marca una tarea como completada o pendiente.
     *
     * @param id         identificador
     * @param completada nuevo estado
     * @return la tarea modificada
     * @throws TareaNoEncontradaException si no existe
     */
    @Transactional
    public TareaResponse marcarCompletada(Long id, boolean completada) {
        Tarea tarea = buscar(id);
        tarea.setCompletada(completada);
        return TareaResponse.de(repositorio.saveAndFlush(tarea), hoy());
    }

    /**
     * Borra una tarea.
     *
     * @param id identificador
     * @throws TareaNoEncontradaException si no existe
     */
    @Transactional
    public void borrar(Long id) {
        repositorio.delete(buscar(id));
    }

    /**
     * Calcula el recuento de tareas por estado.
     *
     * @return el resumen
     */
    public ResumenTareas resumen() {
        long pendientes = repositorio.countByCompletada(false);
        long completadas = repositorio.countByCompletada(true);
        long vencidas = repositorio.countByCompletadaFalseAndFechaLimiteBefore(hoy());
        return new ResumenTareas(pendientes + completadas, pendientes, completadas, vencidas);
    }

    private Tarea buscar(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new TareaNoEncontradaException(id));
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }
}
