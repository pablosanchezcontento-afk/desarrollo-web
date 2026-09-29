package es.pablosanchez.tareas.web;

import es.pablosanchez.tareas.tarea.Prioridad;
import es.pablosanchez.tareas.tarea.Tarea;
import es.pablosanchez.tareas.tarea.TareaRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans de la aplicación: reloj, documentación OpenAPI y datos de ejemplo.
 */
@Configuration
public class Configuracion {

    /**
     * Reloj del sistema. Los tests lo sustituyen por uno fijo.
     *
     * @return reloj en la zona horaria del sistema
     */
    @Bean
    Clock reloj() {
        return Clock.systemDefaultZone();
    }

    /**
     * Metadatos que muestra Swagger UI.
     *
     * @return descripción de la API
     */
    @Bean
    OpenAPI documentacionApi() {
        return new OpenAPI().info(new Info()
                .title("Gestor de tareas")
                .version("1.0.0")
                .description("API REST para gestionar tareas con prioridad y fecha límite.")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }

    /**
     * Crea tareas de ejemplo la primera vez que se arranca, si la base de datos
     * está vacía. Se desactiva con {@code app.datos-ejemplo=false}.
     *
     * @param repositorio repositorio de tareas
     * @param reloj       reloj para calcular las fechas
     * @return tarea que se ejecuta al arrancar
     */
    @Bean
    @ConditionalOnProperty(name = "app.datos-ejemplo", havingValue = "true", matchIfMissing = true)
    ApplicationRunner datosDeEjemplo(TareaRepository repositorio, Clock reloj) {
        return args -> {
            if (repositorio.count() > 0) {
                return;
            }
            LocalDate hoy = LocalDate.now(reloj);
            repositorio.save(new Tarea("Entregar la práctica de DWES",
                    "Subir el repositorio y la memoria en PDF", Prioridad.ALTA, hoy.plusDays(3)));
            repositorio.save(new Tarea("Repasar el tema de JavaScript asíncrono",
                    "Promesas, async/await y fetch", Prioridad.MEDIA, hoy.plusDays(7)));
            repositorio.save(new Tarea("Actualizar el portfolio de GitHub", null, Prioridad.BAJA, null));
            Tarea hecha = new Tarea("Instalar JDK 21 e IntelliJ", null, Prioridad.MEDIA, hoy.minusDays(10));
            hecha.setCompletada(true);
            repositorio.save(hecha);
        };
    }
}
