package es.pablosanchez.tareas.tarea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * Tests de la capa de datos: solo arrancan JPA y una base de datos en memoria,
 * por lo que son mucho más rápidos que un {@code @SpringBootTest}.
 */
@DataJpaTest
@DisplayName("TareaRepository")
class TareaRepositoryTest {

    @Autowired
    TareaRepository repositorio;

    @BeforeEach
    void datos() {
        repositorio.save(new Tarea("Examen de Java", "Tema de colecciones", Prioridad.ALTA, LocalDate.of(2026, 10, 3)));
        repositorio.save(new Tarea("Comprar libros", null, Prioridad.BAJA, null));
        Tarea hecha = new Tarea("Matrícula", "Pagar la tasa", Prioridad.MEDIA, LocalDate.of(2026, 9, 1));
        hecha.setCompletada(true);
        repositorio.save(hecha);
    }

    private List<String> titulos(List<Tarea> tareas) {
        return tareas.stream().map(Tarea::getTitulo).toList();
    }

    @Test
    @DisplayName("sin filtros devuelve todas, pendientes primero")
    void sinFiltros() {
        assertEquals(List.of("Examen de Java", "Comprar libros", "Matrícula"),
                titulos(repositorio.buscar(null, null, null)));
    }

    @Test
    @DisplayName("filtra por estado y prioridad")
    void filtros() {
        assertEquals(List.of("Matrícula"), titulos(repositorio.buscar(true, null, null)));
        assertEquals(List.of("Comprar libros"), titulos(repositorio.buscar(false, Prioridad.BAJA, null)));
    }

    @Test
    @DisplayName("busca texto en la descripción aunque el título no coincida")
    void textoEnDescripcion() {
        assertEquals(List.of("Examen de Java"), titulos(repositorio.buscar(null, null, "colecciones")));
    }

    @Test
    @DisplayName("cuenta pendientes y vencidas")
    void contadores() {
        assertEquals(2, repositorio.countByCompletada(false));
        assertEquals(0, repositorio.countByCompletadaFalseAndFechaLimiteBefore(LocalDate.of(2026, 10, 3)));
        assertEquals(1, repositorio.countByCompletadaFalseAndFechaLimiteBefore(LocalDate.of(2026, 10, 4)));
    }

    @Test
    @DisplayName("rellena las fechas de creación y modificación al guardar")
    void auditoria() {
        Tarea tarea = repositorio.saveAndFlush(new Tarea("Nueva", null, null, null));
        assertNotNull(tarea.getCreadaEn());
        assertEquals(tarea.getCreadaEn(), tarea.getActualizadaEn());
    }
}
