package es.pablosanchez.tareas.tarea;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a las tareas guardadas. Spring Data genera la implementación.
 */
public interface TareaRepository extends JpaRepository<Tarea, Long> {

    /**
     * Busca tareas aplicando los filtros que no sean {@code null}.
     *
     * <p>Orden: primero las pendientes, después por fecha límite (las que no
     * tienen fecha, al final) y por último las más recientes.</p>
     *
     * @param completada filtra por estado, o {@code null} para todas
     * @param prioridad  filtra por prioridad, o {@code null} para todas
     * @param texto      texto a buscar en título o descripción (en minúsculas), o {@code null}
     * @return las tareas que cumplen los filtros
     */
    @Query("""
            select t from Tarea t
            where (:completada is null or t.completada = :completada)
              and (:prioridad is null or t.prioridad = :prioridad)
              and (:texto is null
                   or lower(t.titulo) like concat('%', :texto, '%')
                   or lower(coalesce(t.descripcion, '')) like concat('%', :texto, '%'))
            order by t.completada asc,
                     case when t.fechaLimite is null then 1 else 0 end,
                     t.fechaLimite asc,
                     t.id desc
            """)
    List<Tarea> buscar(@Param("completada") Boolean completada,
                       @Param("prioridad") Prioridad prioridad,
                       @Param("texto") String texto);

    /**
     * Cuenta las tareas según su estado.
     *
     * @param completada estado a contar
     * @return número de tareas
     */
    long countByCompletada(boolean completada);

    /**
     * Cuenta las tareas pendientes con la fecha límite anterior a la indicada.
     *
     * @param fecha fecha de referencia
     * @return número de tareas vencidas
     */
    long countByCompletadaFalseAndFechaLimiteBefore(LocalDate fecha);
}
