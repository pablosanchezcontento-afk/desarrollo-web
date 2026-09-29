// Interfaz del gestor de tareas. Consume la API REST de /api/tareas con fetch.

const API = "/api/tareas";
const PRIORIDADES = { ALTA: "Alta", MEDIA: "Media", BAJA: "Baja" };
const formatoFecha = new Intl.DateTimeFormat("es-ES", { day: "numeric", month: "short", year: "numeric" });

const $ = (selector) => document.querySelector(selector);

// ---------- Acceso a la API ----------

/**
 * Hace una petición a la API y devuelve el JSON de la respuesta.
 * Si la API responde con error, lanza un Error con el mensaje del servidor
 * (formato Problem Details) y los errores por campo en `error.campos`.
 */
async function api(ruta = "", opciones = {}) {
    const respuesta = await fetch(API + ruta, {
        ...opciones,
        headers: { "Content-Type": "application/json", ...opciones.headers },
    });
    if (respuesta.status === 204) {
        return null;
    }
    const datos = await respuesta.json().catch(() => ({}));
    if (!respuesta.ok) {
        const error = new Error(datos.detail || `Error ${respuesta.status}`);
        error.campos = datos.errores || {};
        throw error;
    }
    return datos;
}

function mensajeDeError(error) {
    const campos = Object.values(error.campos || {});
    return campos.length > 0 ? campos.join(". ") : error.message;
}

function datosDelFormulario(form) {
    const datos = Object.fromEntries(new FormData(form));
    return {
        titulo: datos.titulo,
        descripcion: datos.descripcion || null,
        prioridad: datos.prioridad,
        fechaLimite: datos.fechaLimite || null,
    };
}

// ---------- Pintar la lista ----------

function filtrosActuales() {
    const parametros = new URLSearchParams();
    const estado = $("#filtro-estado").value;
    const prioridad = $("#filtro-prioridad").value;
    const texto = $("#filtro-texto").value.trim();
    if (estado) parametros.set("completada", estado);
    if (prioridad) parametros.set("prioridad", prioridad);
    if (texto) parametros.set("q", texto);
    const consulta = parametros.toString();
    return consulta ? `?${consulta}` : "";
}

function crearElementoTarea(tarea) {
    const nodo = $("#plantilla-tarea").content.firstElementChild.cloneNode(true);
    nodo.dataset.id = tarea.id;
    nodo.classList.toggle("completada", tarea.completada);

    const check = nodo.querySelector(".tarea-check");
    check.checked = tarea.completada;
    check.setAttribute("aria-label", `Marcar «${tarea.titulo}» como ${tarea.completada ? "pendiente" : "completada"}`);
    check.addEventListener("change", () => cambiarEstado(tarea.id, check.checked));

    nodo.querySelector(".tarea-titulo").textContent = tarea.titulo;
    nodo.querySelector(".tarea-descripcion").textContent = tarea.descripcion ?? "";

    const prioridad = nodo.querySelector(".prioridad");
    prioridad.textContent = PRIORIDADES[tarea.prioridad];
    prioridad.classList.add(tarea.prioridad);

    const fecha = nodo.querySelector(".fecha");
    if (tarea.fechaLimite) {
        // Se añade la hora para que la fecha no cambie de día por la zona horaria.
        const texto = formatoFecha.format(new Date(`${tarea.fechaLimite}T00:00`));
        fecha.textContent = tarea.vencida ? `Vencida: ${texto}` : texto;
        fecha.classList.toggle("vencida", tarea.vencida);
    }

    nodo.querySelector(".boton-editar").addEventListener("click", () => abrirEdicion(tarea));
    nodo.querySelector(".boton-borrar").addEventListener("click", () => borrar(tarea));
    return nodo;
}

async function cargar() {
    try {
        const [tareas, resumen] = await Promise.all([api(filtrosActuales()), api("/resumen")]);
        $("#lista").replaceChildren(...tareas.map(crearElementoTarea));
        $("#vacio").hidden = tareas.length > 0;
        $("#res-pendientes").textContent = resumen.pendientes;
        $("#res-completadas").textContent = resumen.completadas;
        $("#res-vencidas").textContent = resumen.vencidas;
        $("#error-lista").textContent = "";
    } catch (error) {
        $("#error-lista").textContent = `No se pudieron cargar las tareas: ${mensajeDeError(error)}`;
    }
}

// ---------- Acciones ----------

async function crear(evento) {
    evento.preventDefault();
    const form = evento.currentTarget;
    try {
        await api("", { method: "POST", body: JSON.stringify(datosDelFormulario(form)) });
        form.reset();
        $("#error-nueva").textContent = "";
        $("#nueva-titulo").focus();
        await cargar();
    } catch (error) {
        $("#error-nueva").textContent = mensajeDeError(error);
    }
}

async function cambiarEstado(id, completada) {
    try {
        await api(`/${id}/completada`, { method: "PATCH", body: JSON.stringify({ completada }) });
    } catch (error) {
        $("#error-lista").textContent = mensajeDeError(error);
    }
    await cargar();
}

async function borrar(tarea) {
    if (!confirm(`¿Borrar «${tarea.titulo}»?`)) {
        return;
    }
    try {
        await api(`/${tarea.id}`, { method: "DELETE" });
    } catch (error) {
        $("#error-lista").textContent = mensajeDeError(error);
    }
    await cargar();
}

function abrirEdicion(tarea) {
    const form = $("#form-editar");
    form.elements.id.value = tarea.id;
    form.elements.titulo.value = tarea.titulo;
    form.elements.descripcion.value = tarea.descripcion ?? "";
    form.elements.prioridad.value = tarea.prioridad;
    form.elements.fechaLimite.value = tarea.fechaLimite ?? "";
    $("#error-editar").textContent = "";
    $("#dialogo-editar").showModal();
}

async function guardarEdicion(evento) {
    evento.preventDefault();
    const form = evento.currentTarget;
    try {
        await api(`/${form.elements.id.value}`, { method: "PUT", body: JSON.stringify(datosDelFormulario(form)) });
        $("#dialogo-editar").close();
        await cargar();
    } catch (error) {
        $("#error-editar").textContent = mensajeDeError(error);
    }
}

// ---------- Inicio ----------

function conRetardo(funcion, ms) {
    let temporizador;
    return (...args) => {
        clearTimeout(temporizador);
        temporizador = setTimeout(() => funcion(...args), ms);
    };
}

$("#form-nueva").addEventListener("submit", crear);
$("#form-editar").addEventListener("submit", guardarEdicion);
$("#cancelar-editar").addEventListener("click", () => $("#dialogo-editar").close());
$("#filtro-estado").addEventListener("change", cargar);
$("#filtro-prioridad").addEventListener("change", cargar);
$("#filtro-texto").addEventListener("input", conRetardo(cargar, 250));

cargar();
