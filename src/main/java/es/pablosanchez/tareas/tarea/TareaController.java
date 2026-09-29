package es.pablosanchez.tareas.tarea;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de tareas.
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>GET</td><td>/api/tareas</td><td>listar (filtros opcionales)</td></tr>
 *   <tr><td>GET</td><td>/api/tareas/resumen</td><td>recuento por estado</td></tr>
 *   <tr><td>GET</td><td>/api/tareas/{id}</td><td>obtener una</td></tr>
 *   <tr><td>POST</td><td>/api/tareas</td><td>crear</td></tr>
 *   <tr><td>PUT</td><td>/api/tareas/{id}</td><td>modificar</td></tr>
 *   <tr><td>PATCH</td><td>/api/tareas/{id}/completada</td><td>marcar como hecha o pendiente</td></tr>
 *   <tr><td>DELETE</td><td>/api/tareas/{id}</td><td>borrar</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/tareas")
@Tag(name = "Tareas", description = "Crear, consultar, modificar y borrar tareas")
public class TareaController {

    private final TareaService servicio;

    /**
     * Crea el controlador.
     *
     * @param servicio lógica de negocio
     */
    public TareaController(TareaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar tareas", description = "Pendientes primero y ordenadas por fecha límite.")
    public List<TareaResponse> listar(
            @Parameter(description = "true = completadas, false = pendientes")
            @RequestParam(required = false) Boolean completada,
            @RequestParam(required = false) Prioridad prioridad,
            @Parameter(description = "Texto a buscar en título o descripción")
            @RequestParam(required = false) String q) {
        return servicio.listar(completada, prioridad, q);
    }

    @GetMapping("/resumen")
    @Operation(summary = "Recuento de tareas por estado")
    public ResumenTareas resumen() {
        return servicio.resumen();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una tarea")
    public TareaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear una tarea")
    public ResponseEntity<TareaResponse> crear(@Valid @RequestBody TareaRequest datos) {
        TareaResponse creada = servicio.crear(datos);
        return ResponseEntity.created(URI.create("/api/tareas/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar una tarea")
    public TareaResponse actualizar(@PathVariable Long id, @Valid @RequestBody TareaRequest datos) {
        return servicio.actualizar(id, datos);
    }

    @PatchMapping("/{id}/completada")
    @Operation(summary = "Marcar una tarea como completada o pendiente")
    public TareaResponse marcarCompletada(@PathVariable Long id, @RequestBody EstadoRequest estado) {
        return servicio.marcarCompletada(id, estado.completada());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Borrar una tarea")
    public ResponseEntity<Void> borrar(@PathVariable Long id) {
        servicio.borrar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cuerpo de la petición para cambiar el estado.
     *
     * @param completada nuevo estado
     */
    public record EstadoRequest(boolean completada) {
    }
}
