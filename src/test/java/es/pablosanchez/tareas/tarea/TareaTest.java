package es.pablosanchez.tareas.tarea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Tarea (entidad)")
class TareaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    @Test
    @DisplayName("normaliza título, descripción vacía y prioridad nula")
    void normaliza() {
        Tarea tarea = new Tarea("  Estudiar  ", "   ", null, null);
        assertEquals("Estudiar", tarea.getTitulo());
        assertNull(tarea.getDescripcion());
        assertEquals(Prioridad.MEDIA, tarea.getPrioridad());
    }

    @ParameterizedTest(name = "fecha límite {0}, completada {1} -> vencida {2}")
    @CsvSource({
            "2026-09-30, false, true",
            "2026-10-01, false, false",
            "2026-10-02, false, false",
            "2026-09-30, true,  false",
            ",           false, false"
    })
    @DisplayName("estaVencida solo es cierto si está pendiente y la fecha ya pasó")
    void vencida(LocalDate fechaLimite, boolean completada, boolean esperado) {
        Tarea tarea = new Tarea("X", null, Prioridad.ALTA, fechaLimite);
        tarea.setCompletada(completada);
        assertEquals(esperado, tarea.estaVencida(HOY));
    }

    @Test
    @DisplayName("una tarea nueva está pendiente")
    void nuevaPendiente() {
        Tarea tarea = new Tarea("X", null, Prioridad.BAJA, null);
        assertFalse(tarea.isCompletada());
        tarea.setCompletada(true);
        assertTrue(tarea.isCompletada());
    }
}
