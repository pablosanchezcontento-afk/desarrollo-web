package es.pablosanchez.tareas.tarea;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Reloj fijo para los tests: "hoy" es siempre el 1 de octubre de 2026. */
@TestConfiguration
public class RelojFijo {

    public static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    @Bean
    @Primary
    Clock relojDeTest() {
        return Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
    }
}
