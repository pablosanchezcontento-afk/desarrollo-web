package es.pablosanchez.tareas.tarea;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Tarea guardada en la base de datos.
 *
 * <p>La API nunca expone esta entidad directamente: usa {@link TareaRequest}
 * para la entrada y {@link TareaResponse} para la salida, de modo que el
 * modelo de base de datos puede cambiar sin romper a los clientes.</p>
 */
@Entity
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = TareaRequest.MAX_TITULO)
    private String titulo;

    @Column(length = TareaRequest.MAX_DESCRIPCION)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridad prioridad = Prioridad.MEDIA;

    private LocalDate fechaLimite;

    @Column(nullable = false)
    private boolean completada;

    @Column(nullable = false, updatable = false)
    private Instant creadaEn;

    @Column(nullable = false)
    private Instant actualizadaEn;

    /** Constructor requerido por JPA. */
    protected Tarea() {
    }

    /**
     * Crea una tarea pendiente.
     *
     * @param titulo      título obligatorio
     * @param descripcion descripción opcional
     * @param prioridad   prioridad; si es {@code null} se usa {@link Prioridad#MEDIA}
     * @param fechaLimite fecha límite opcional
     */
    public Tarea(String titulo, String descripcion, Prioridad prioridad, LocalDate fechaLimite) {
        actualizar(titulo, descripcion, prioridad, fechaLimite);
    }

    /**
     * Sustituye los datos editables de la tarea.
     *
     * @param titulo      nuevo título
     * @param descripcion nueva descripción
     * @param prioridad   nueva prioridad; si es {@code null} se usa {@link Prioridad#MEDIA}
     * @param fechaLimite nueva fecha límite
     */
    public void actualizar(String titulo, String descripcion, Prioridad prioridad, LocalDate fechaLimite) {
        this.titulo = titulo.strip();
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
        this.prioridad = prioridad == null ? Prioridad.MEDIA : prioridad;
        this.fechaLimite = fechaLimite;
    }

    @PrePersist
    void alCrear() {
        creadaEn = Instant.now();
        actualizadaEn = creadaEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadaEn = Instant.now();
    }

    /**
     * Indica si la tarea está pendiente y su fecha límite ya ha pasado.
     *
     * @param hoy fecha de referencia
     * @return {@code true} si está vencida
     */
    public boolean estaVencida(LocalDate hoy) {
        return !completada && fechaLimite != null && fechaLimite.isBefore(hoy);
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public boolean isCompletada() {
        return completada;
    }

    public void setCompletada(boolean completada) {
        this.completada = completada;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getActualizadaEn() {
        return actualizadaEn;
    }
}
