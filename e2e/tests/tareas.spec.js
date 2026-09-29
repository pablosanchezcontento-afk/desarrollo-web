import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

async function vaciar(request) {
    const tareas = await (await request.get("/api/tareas")).json();
    for (const tarea of tareas) {
        await request.delete(`/api/tareas/${tarea.id}`);
    }
}

test.beforeEach(async ({ page, request }) => {
    await vaciar(request);
    const erroresCsp = [];
    page.on("console", (mensaje) => {
        if (mensaje.type() === "error" && /Content Security Policy|Refused to/.test(mensaje.text())) erroresCsp.push(mensaje.text());
    });
    page.erroresConsola = erroresCsp;
    await page.goto("/");
    await expect(page.locator("#res-pendientes")).toHaveText("0");
});

test.afterEach(async ({ page }) => {
    // Con la CSP estricta activa, la página no debe registrar errores (p. ej. recursos bloqueados).
    expect(page.erroresConsola).toEqual([]);
});

async function crearTarea(page, { titulo, descripcion = "", prioridad = "MEDIA", fecha = "" }) {
    await page.locator("#nueva-titulo").fill(titulo);
    await page.locator("#nueva-descripcion").fill(descripcion);
    await page.locator("#nueva-prioridad").selectOption(prioridad);
    await page.locator("#nueva-fecha").fill(fecha);
    await page.getByRole("button", { name: "Añadir tarea" }).click();
    await expect(page.locator(".tarea-titulo", { hasText: titulo })).toBeVisible();
}

test("crear una tarea la muestra en la lista y actualiza el resumen", async ({ page }) => {
    await crearTarea(page, { titulo: "Entregar la práctica", descripcion: "Con memoria", prioridad: "ALTA", fecha: "2099-10-15" });
    const tarea = page.locator(".tarea", { hasText: "Entregar la práctica" });
    await expect(tarea.locator(".tarea-descripcion")).toHaveText("Con memoria");
    await expect(tarea.locator(".prioridad")).toHaveText("Alta");
    await expect(page.locator("#res-pendientes")).toHaveText("1");
    await expect(page.locator("#nueva-titulo")).toHaveValue("");
});

test("el servidor valida: título vacío muestra el error", async ({ page }) => {
    await page.getByRole("button", { name: "Añadir tarea" }).click();
    await expect(page.locator("#error-nueva")).toContainText("título");
    await expect(page.locator(".tarea")).toHaveCount(0);
});

test("una tarea con fecha pasada aparece como vencida", async ({ page }) => {
    await crearTarea(page, { titulo: "Atrasada", fecha: "2020-01-01" });
    await expect(page.locator(".tarea", { hasText: "Atrasada" }).locator(".fecha")).toContainText("Vencida");
    await expect(page.locator("#res-vencidas")).toHaveText("1");
});

test("completar una tarea la mueve a completadas", async ({ page }) => {
    await crearTarea(page, { titulo: "Hacer la compra" });
    await page.locator(".tarea", { hasText: "Hacer la compra" }).locator(".tarea-check").check();
    await expect(page.locator("#res-completadas")).toHaveText("1");
    await expect(page.locator(".tarea", { hasText: "Hacer la compra" })).toHaveCount(0); // filtro: pendientes
    await page.locator("#filtro-estado").selectOption("true");
    await expect(page.locator(".tarea", { hasText: "Hacer la compra" })).toBeVisible();
});

test("editar una tarea desde el diálogo", async ({ page }) => {
    await crearTarea(page, { titulo: "Borrador" });
    await page.locator(".tarea", { hasText: "Borrador" }).getByRole("button", { name: "Editar" }).click();
    const dialogo = page.locator("#dialogo-editar");
    await expect(dialogo).toBeVisible();
    await dialogo.locator("#editar-titulo").fill("Versión final");
    await dialogo.locator("#editar-prioridad").selectOption("BAJA");
    await dialogo.getByRole("button", { name: "Guardar" }).click();
    await expect(dialogo).toBeHidden();
    const tarea = page.locator(".tarea", { hasText: "Versión final" });
    await expect(tarea.locator(".prioridad")).toHaveText("Baja");
});

test("borrar pide confirmación", async ({ page }) => {
    await crearTarea(page, { titulo: "Temporal" });
    page.once("dialog", (dialogo) => dialogo.dismiss());
    await page.locator(".tarea", { hasText: "Temporal" }).getByRole("button", { name: "Borrar" }).click();
    await expect(page.locator(".tarea", { hasText: "Temporal" })).toBeVisible();
    page.once("dialog", (dialogo) => dialogo.accept());
    await page.locator(".tarea", { hasText: "Temporal" }).getByRole("button", { name: "Borrar" }).click();
    await expect(page.locator(".tarea", { hasText: "Temporal" })).toHaveCount(0);
});

test("búsqueda y filtro por prioridad", async ({ page }) => {
    await crearTarea(page, { titulo: "Estudiar Java", prioridad: "ALTA" });
    await crearTarea(page, { titulo: "Estudiar JavaScript", prioridad: "BAJA" });
    await crearTarea(page, { titulo: "Ir al gimnasio", prioridad: "ALTA" });
    await page.locator("#filtro-texto").fill("estudiar");
    await expect(page.locator(".tarea")).toHaveCount(2);
    await page.locator("#filtro-prioridad").selectOption("ALTA");
    await expect(page.locator(".tarea")).toHaveCount(1);
    await expect(page.locator(".tarea-titulo")).toHaveText("Estudiar Java");
    await page.locator("#filtro-texto").fill("nada que coincida");
    await expect(page.locator("#vacio")).toBeVisible();
});

test("el texto de una tarea se muestra literal (sin inyección de HTML)", async ({ page }) => {
    const titulo = '<img src=x onerror="window.hackeado=1">';
    await crearTarea(page, { titulo });
    await expect(page.locator(".tarea-titulo")).toHaveText(titulo);
    expect(await page.evaluate(() => window.hackeado)).toBeUndefined();
});

test("sin errores de accesibilidad (axe, WCAG 2.1 AA)", async ({ page }) => {
    await crearTarea(page, { titulo: "Revisar accesibilidad", prioridad: "ALTA", fecha: "2020-01-01" });
    await crearTarea(page, { titulo: "Media", prioridad: "MEDIA" });
    await crearTarea(page, { titulo: "Baja", prioridad: "BAJA" });
    const resultado = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
    expect(resultado.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target).join(" ")}`)).toEqual([]);
});

test("sin errores de accesibilidad en modo oscuro", async ({ page }) => {
    await page.emulateMedia({ colorScheme: "dark" });
    await crearTarea(page, { titulo: "Modo oscuro", prioridad: "ALTA", fecha: "2020-01-01" });
    await crearTarea(page, { titulo: "Media", prioridad: "MEDIA" });
    await crearTarea(page, { titulo: "Baja", prioridad: "BAJA" });
    const resultado = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
    expect(resultado.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target).join(" ")}`)).toEqual([]);
});
