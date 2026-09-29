package es.pablosanchez.tareas.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Añade cabeceras de seguridad a todas las respuestas.
 *
 * <p>La política de contenido (CSP) solo permite recursos del propio origen, sin
 * scripts ni estilos en línea: el frontend no los necesita. Swagger UI y la consola
 * de H2 usan scripts en línea y marcos, así que a esas rutas no se les aplica la CSP
 * estricta (la consola de H2 está desactivada en el perfil {@code prod}).</p>
 */
@Component
public class CabecerasSeguridad extends OncePerRequestFilter {

    static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        respuesta.setHeader("X-Content-Type-Options", "nosniff");
        respuesta.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        respuesta.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        if (!excluida(peticion.getRequestURI())) {
            respuesta.setHeader("Content-Security-Policy", CSP);
            respuesta.setHeader("X-Frame-Options", "DENY");
        }
        cadena.doFilter(peticion, respuesta);
    }

    static boolean excluida(String ruta) {
        return ruta.startsWith("/swagger-ui") || ruta.startsWith("/v3/api-docs") || ruta.startsWith("/h2-console");
    }
}
