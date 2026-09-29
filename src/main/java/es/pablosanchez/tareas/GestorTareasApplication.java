package es.pablosanchez.tareas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación.
 *
 * <p>Arranca un servidor web en {@code http://localhost:8080} que sirve la
 * interfaz web ({@code /}), la API REST ({@code /api/tareas}) y su
 * documentación interactiva ({@code /swagger-ui.html}).</p>
 */
@SpringBootApplication
public class GestorTareasApplication {

    /**
     * Arranca la aplicación.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(GestorTareasApplication.class, args);
    }
}
