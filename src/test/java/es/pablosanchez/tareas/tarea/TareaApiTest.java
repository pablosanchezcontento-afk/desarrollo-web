package es.pablosanchez.tareas.tarea;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Tests de integración de la API: arrancan la aplicación completa con una base
 * de datos H2 en memoria y hacen peticiones HTTP simuladas con MockMvc.
 */
@SpringBootTest(properties = {
        "app.datos-ejemplo=false",
        "spring.datasource.url=jdbc:h2:mem:api;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Import(TareaApiTest.RelojFijo.class)
@DisplayName("API /api/tareas")
class TareaApiTest {

    /** "Hoy" es siempre el 1 de octubre de 2026 en estos tests. */
    static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDeTest() {
            return Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    TareaRepository repositorio;

    @BeforeEach
    void limpiar() {
        repositorio.deleteAll();
    }

    private ResultActions crear(String json) throws Exception {
        return mvc.perform(post("/api/tareas").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long crearTarea(String titulo, String prioridad, String fechaLimite) throws Exception {
        String fecha = fechaLimite == null ? "null" : "\"" + fechaLimite + "\"";
        String json = """
                {"titulo": "%s", "prioridad": "%s", "fechaLimite": %s}
                """.formatted(titulo, prioridad, fecha);
        String cuerpo = crear(json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(cuerpo.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Nested
    @DisplayName("POST crea tareas")
    class Crear {

        @Test
        @DisplayName("devuelve 201, la cabecera Location y la tarea creada")
        void creaTarea() throws Exception {
            crear("""
                    {"titulo": "  Estudiar Spring  ", "descripcion": "Capítulo 3",
                     "prioridad": "ALTA", "fechaLimite": "2026-10-05"}
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/tareas/")))
                    .andExpect(jsonPath("$.id").value(notNullValue()))
                    .andExpect(jsonPath("$.titulo").value("Estudiar Spring"))
                    .andExpect(jsonPath("$.descripcion").value("Capítulo 3"))
                    .andExpect(jsonPath("$.prioridad").value("ALTA"))
                    .andExpect(jsonPath("$.fechaLimite").value("2026-10-05"))
                    .andExpect(jsonPath("$.completada").value(false))
                    .andExpect(jsonPath("$.vencida").value(false))
                    .andExpect(jsonPath("$.creadaEn").value(notNullValue()));
        }

        @Test
        @DisplayName("sin prioridad usa MEDIA")
        void prioridadPorDefecto() throws Exception {
            crear("{\"titulo\": \"Algo\"}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.prioridad").value("MEDIA"));
        }

        @Test
        @DisplayName("un título vacío devuelve 400 con el error del campo")
        void tituloObligatorio() throws Exception {
            crear("{\"titulo\": \"   \"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Datos no válidos"))
                    .andExpect(jsonPath("$.errores.titulo").value("El título es obligatorio"));
        }

        @Test
        @DisplayName("un título demasiado largo devuelve 400")
        void tituloLargo() throws Exception {
            crear("{\"titulo\": \"" + "x".repeat(121) + "\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.titulo").value("El título no puede superar 120 caracteres"));
        }

        @Test
        @DisplayName("una prioridad inexistente devuelve 400")
        void prioridadInvalida() throws Exception {
            crear("{\"titulo\": \"Algo\", \"prioridad\": \"URGENTISIMA\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Petición mal formada"));
        }

        @Test
        @DisplayName("una fecha con formato incorrecto devuelve 400")
        void fechaInvalida() throws Exception {
            crear("{\"titulo\": \"Algo\", \"fechaLimite\": \"15/10/2026\"}")
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET consulta tareas")
    class Consultar {

        @Test
        @DisplayName("obtiene una tarea por id")
        void obtenerPorId() throws Exception {
            long id = crearTarea("Leer", "BAJA", null);
            mvc.perform(get("/api/tareas/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo").value("Leer"));
        }

        @Test
        @DisplayName("un id inexistente devuelve 404 en formato Problem Details")
        void noEncontrada() throws Exception {
            mvc.perform(get("/api/tareas/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("No existe ninguna tarea con id 999"));
        }

        @Test
        @DisplayName("un id que no es un número devuelve 400")
        void idNoNumerico() throws Exception {
            mvc.perform(get("/api/tareas/abc")).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("lista las pendientes primero y ordenadas por fecha límite")
        void orden() throws Exception {
            long sinFecha = crearTarea("Sin fecha", "MEDIA", null);
            crearTarea("Lejana", "MEDIA", "2026-12-01");
            crearTarea("Próxima", "MEDIA", "2026-10-02");
            mvc.perform(patch("/api/tareas/{id}/completada", sinFecha)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"completada\": true}"));

            mvc.perform(get("/api/tareas"))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].titulo").value("Próxima"))
                    .andExpect(jsonPath("$[1].titulo").value("Lejana"))
                    .andExpect(jsonPath("$[2].titulo").value("Sin fecha"));
        }

        @Test
        @DisplayName("filtra por estado, prioridad y texto")
        void filtros() throws Exception {
            crearTarea("Examen de BBDD", "ALTA", null);
            crearTarea("Comprar pan", "BAJA", null);
            long hecha = crearTarea("Examen de inglés", "ALTA", null);
            mvc.perform(patch("/api/tareas/{id}/completada", hecha)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"completada\": true}"));

            mvc.perform(get("/api/tareas").param("prioridad", "ALTA"))
                    .andExpect(jsonPath("$", hasSize(2)));
            mvc.perform(get("/api/tareas").param("completada", "false").param("prioridad", "ALTA"))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].titulo").value("Examen de BBDD"));
            mvc.perform(get("/api/tareas").param("q", "EXAMEN"))
                    .andExpect(jsonPath("$", hasSize(2)));
            mvc.perform(get("/api/tareas").param("q", "   "))
                    .andExpect(jsonPath("$", hasSize(3)));
        }

        @Test
        @DisplayName("marca como vencidas las pendientes con fecha pasada")
        void vencidas() throws Exception {
            crearTarea("Atrasada", "ALTA", HOY.minusDays(1).toString());
            crearTarea("Para hoy", "ALTA", HOY.toString());

            mvc.perform(get("/api/tareas"))
                    .andExpect(jsonPath("$[0].titulo").value("Atrasada"))
                    .andExpect(jsonPath("$[0].vencida").value(true))
                    .andExpect(jsonPath("$[1].vencida").value(false));
        }

        @Test
        @DisplayName("el resumen cuenta pendientes, completadas y vencidas")
        void resumen() throws Exception {
            crearTarea("A", "ALTA", HOY.minusDays(3).toString());
            crearTarea("B", "BAJA", null);
            long c = crearTarea("C", "MEDIA", HOY.minusDays(3).toString());
            mvc.perform(patch("/api/tareas/{id}/completada", c)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"completada\": true}"));

            mvc.perform(get("/api/tareas/resumen"))
                    .andExpect(jsonPath("$.total").value(3))
                    .andExpect(jsonPath("$.pendientes").value(2))
                    .andExpect(jsonPath("$.completadas").value(1))
                    .andExpect(jsonPath("$.vencidas").value(1));
        }
    }

    @Nested
    @DisplayName("PUT, PATCH y DELETE modifican tareas")
    class Modificar {

        @Test
        @DisplayName("PUT sustituye los datos y borra la descripción vacía")
        void actualizar() throws Exception {
            long id = crearTarea("Viejo", "BAJA", null);
            mvc.perform(put("/api/tareas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                            {"titulo": "Nuevo", "descripcion": "  ", "prioridad": "ALTA", "fechaLimite": "2026-11-11"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo").value("Nuevo"))
                    .andExpect(jsonPath("$.descripcion").doesNotExist())
                    .andExpect(jsonPath("$.prioridad").value("ALTA"))
                    .andExpect(jsonPath("$.fechaLimite").value("2026-11-11"));
        }

        @Test
        @DisplayName("PUT valida igual que POST")
        void actualizarValida() throws Exception {
            long id = crearTarea("Viejo", "BAJA", null);
            mvc.perform(put("/api/tareas/{id}", id).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"titulo\": \"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PUT sobre un id inexistente devuelve 404")
        void actualizarInexistente() throws Exception {
            mvc.perform(put("/api/tareas/999").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"titulo\": \"X\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PATCH marca y desmarca una tarea como completada")
        void completar() throws Exception {
            long id = crearTarea("Hacer", "MEDIA", HOY.minusDays(1).toString());
            mvc.perform(patch("/api/tareas/{id}/completada", id)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"completada\": true}"))
                    .andExpect(jsonPath("$.completada").value(true))
                    .andExpect(jsonPath("$.vencida").value(false));
            mvc.perform(patch("/api/tareas/{id}/completada", id)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"completada\": false}"))
                    .andExpect(jsonPath("$.completada").value(false))
                    .andExpect(jsonPath("$.vencida").value(true));
        }

        @Test
        @DisplayName("DELETE borra la tarea y devuelve 204")
        void borrar() throws Exception {
            long id = crearTarea("Borrar", "BAJA", null);
            mvc.perform(delete("/api/tareas/{id}", id)).andExpect(status().isNoContent());
            mvc.perform(get("/api/tareas/{id}", id)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE sobre un id inexistente devuelve 404")
        void borrarInexistente() throws Exception {
            mvc.perform(delete("/api/tareas/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Otros recursos")
    class Otros {

        @Test
        @DisplayName("sirve la interfaz web en /")
        void interfazWeb() throws Exception {
            mvc.perform(get("/index.html"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().string(containsString("Gestor de tareas")));
        }

        @Test
        @DisplayName("publica la especificación OpenAPI")
        void openApi() throws Exception {
            mvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.title").value("Gestor de tareas"))
                    .andExpect(jsonPath("$.paths['/api/tareas']").exists());
        }

        @Test
        @DisplayName("el endpoint de salud responde UP")
        void salud() throws Exception {
            mvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }
}
