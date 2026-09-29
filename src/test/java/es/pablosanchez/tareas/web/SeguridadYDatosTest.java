package es.pablosanchez.tareas.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.pablosanchez.tareas.tarea.RelojFijo;
import es.pablosanchez.tareas.tarea.Tarea;
import es.pablosanchez.tareas.tarea.TareaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.datos-ejemplo=true",
        "spring.datasource.url=jdbc:h2:mem:seguridad;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Import(RelojFijo.class)
@DisplayName("Configuración: datos de ejemplo y cabeceras de seguridad")
class SeguridadYDatosTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TareaRepository repositorio;

    @Autowired
    ApplicationRunner datosDeEjemplo;

    @Test
    void creaCuatroTareasDeEjemploUnaSolaVez() throws Exception {
        List<Tarea> tareas = repositorio.findAll();
        assertThat(tareas).hasSize(4);
        assertThat(tareas).filteredOn(Tarea::isCompletada).hasSize(1);
        assertThat(tareas).extracting(Tarea::getFechaLimite).contains(RelojFijo.HOY.plusDays(3));

        ApplicationArguments sinArgumentos = new DefaultApplicationArguments();
        datosDeEjemplo.run(sinArgumentos);
        assertThat(repositorio.count()).isEqualTo(4);
    }

    @Test
    void laPaginaYLaApiLlevanCabecerasDeSeguridad() throws Exception {
        for (String ruta : List.of("/", "/api/tareas")) {
            mvc.perform(get(ruta))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Security-Policy", CabecerasSeguridad.CSP))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
        }
    }

    @Test
    void swaggerNoRecibeLaCspEstricta() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Security-Policy"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        assertThat(CabecerasSeguridad.excluida("/swagger-ui/index.html")).isTrue();
        assertThat(CabecerasSeguridad.excluida("/h2-console")).isTrue();
        assertThat(CabecerasSeguridad.excluida("/api/tareas")).isFalse();
    }
}
