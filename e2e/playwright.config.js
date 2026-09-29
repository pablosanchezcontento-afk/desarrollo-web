import { defineConfig, devices } from "@playwright/test";

// Arranca el .jar ya compilado (mvn package) con una base de datos en memoria vacía.
const jar = process.env.JAR ?? "../target/gestor-tareas-1.0.0.jar";

export default defineConfig({
    testDir: "tests",
    workers: 1, // Todas las pruebas comparten la misma base de datos.
    retries: process.env.CI ? 1 : 0,
    reporter: process.env.CI ? [["github"], ["html", { open: "never" }]] : "list",
    use: {
        baseURL: "http://127.0.0.1:8089",
        locale: "es-ES",
        launchOptions: process.env.PW_CHROMIUM ? { executablePath: process.env.PW_CHROMIUM } : {},
    },
    projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
    webServer: {
        command:
            `java -jar ${jar} --server.port=8089 --app.datos-ejemplo=false ` +
            "--spring.datasource.url=jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1 --spring.h2.console.enabled=false",
        url: "http://127.0.0.1:8089/actuator/health",
        timeout: 120_000,
        reuseExistingServer: false,
    },
});
