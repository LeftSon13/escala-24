const $ = (selector, parent = document) => parent.querySelector(selector);
const $$ = (selector, parent = document) => [...parent.querySelectorAll(selector)];

const state = { user: null, holidays: [], firefighters: [], unavailabilities: [], schedule: null };
const pages = {
    dashboard: $("#dashboard-page"), schedules: $("#schedules-page"),
    firefighters: $("#firefighters-page"), unavailabilities: $("#unavailabilities-page"),
    holidays: $("#holiday-page"), settings: $("#settings-page")
};
const pageNames = { dashboard: "Visão geral", schedules: "Escalas mensais", firefighters: "Bombeiros", unavailabilities: "Indisponibilidades", holidays: "Feriados", settings: "Configurações" };
const labels = {
    roles: { ADMIN: "Administrador", FIREFIGHTER: "Bombeiro" },
    types: { VACATION: "Férias", MEDICAL_LEAVE: "Licença médica", PERSONAL_COMMITMENT: "Compromisso pessoal", OTHER: "Outro" },
    statuses: { PENDING: "Pendente", APPROVED: "Aprovada", REJECTED: "Rejeitada", DRAFT: "Rascunho", PUBLISHED: "Publicada" },
    dayTypes: { WEEKDAY: "Dia útil", WEEKEND_OR_HOLIDAY: "Fim de semana ou feriado" }
};
const monthNames = ["Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"];

const safeHttpMethods = new Set([
    "GET",
    "HEAD",
    "OPTIONS",
    "TRACE"
]);

function readCookie(name) {
    const prefix = `${encodeURIComponent(name)}=`;
    const cookie = document.cookie
        .split("; ")
        .find((item) => item.startsWith(prefix));

    return cookie
        ? decodeURIComponent(cookie.substring(prefix.length))
        : null;
}

async function getCsrfToken() {
    const cookieToken = readCookie("XSRF-TOKEN");

    if (cookieToken) {
        return cookieToken;
    }

    const response = await fetch("/api/auth/csrf", {
        credentials: "same-origin",
        headers: {
            Accept: "application/json"
        }
    });

    if (!response.ok) {
        throw new Error(
            "Não foi possível iniciar a proteção da sessão."
        );
    }

    const body = await response.json();

    return readCookie("XSRF-TOKEN") ?? body.token;
}

async function apiRequest(path, options = {}) {
    const headers = { ...options.headers };
    const method = (options.method ?? "GET").toUpperCase();

    if (options.body && !(options.body instanceof FormData)) headers["Content-Type"] = "application/json";

    if (!safeHttpMethods.has(method)) {
        headers["X-XSRF-TOKEN"] = await getCsrfToken();
    }

    const response = await fetch(path, { credentials: "same-origin", ...options, headers });
    if (response.status === 204) return null;
    const isJson = (response.headers.get("content-type") ?? "").includes("application/json");
    const body = isJson ? await response.json() : null;
    if (!response.ok) {
        const error = new Error(body?.message ?? "Não foi possível concluir a operação.");
        error.status = response.status;
        error.fieldErrors = body?.fieldErrors ?? {};
        if (response.status === 401 && path !== "/api/auth/login") closeDashboard();
        throw error;
    }
    return body;
}

function escapeHtml(value = "") {
    return String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#039;");
}
function formatDate(value) { if (!value) return "—"; const [y, m, d] = value.substring(0, 10).split("-"); return `${d}/${m}/${y}`; }
function formatDateTime(value) { return value ? new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(new Date(value)) : "—"; }
function getInitials(name) { return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join("").toUpperCase(); }

let toastTimeout;
function showToast(message, type = "success") {
    $("#toast").dataset.type = type; $("#toast-message").textContent = message; $("#toast").classList.remove("hidden");
    clearTimeout(toastTimeout); toastTimeout = setTimeout(() => $("#toast").classList.add("hidden"), 3600);
}
function setLoading(button, active, text) {
    if (!button.dataset.originalText) button.dataset.originalText = button.textContent.trim();
    button.disabled = active; button.textContent = active ? text : button.dataset.originalText;
}
function clearErrors(form) { $$(".field-error", form).forEach((item) => item.textContent = ""); }
function showErrors(form, errors = {}) { clearErrors(form); Object.entries(errors).forEach(([field, message]) => { const item = $(`[data-error-for="${field}"]`, form); if (item) item.textContent = message; }); }

function applyUser(user) {
    state.user = user;
    $(".profile-copy strong").textContent = user.name;
    $(".profile-copy span").textContent = labels.roles[user.role] ?? user.role;
    $(".sidebar-profile .avatar").textContent = getInitials(user.name);
    $(".welcome-banner h2").textContent = `Olá, ${user.name.split(/\s+/)[0]}.`;
    $$('[data-role="ADMIN"], .admin-only').forEach((item) => item.classList.toggle("hidden", user.role !== "ADMIN"));
    $$(".firefighter-only").forEach((item) => item.classList.toggle("hidden", user.role !== "FIREFIGHTER"));
    $("#generate-button").classList.toggle("hidden", user.role !== "ADMIN");
}
function openDashboard(user) {
    applyUser(user); $("#login-screen").classList.add("hidden"); $("#app-shell").classList.remove("hidden");
    if (user.mustChangePassword) { $("#password-change-notice").textContent = "Sua senha é temporária. Altere-a para liberar os demais módulos."; navigateTo("settings"); showToast("A troca de senha é obrigatória.", "warning"); }
    else navigateTo("dashboard");
}
function closeDashboard() { state.user = null; $("#app-shell").classList.add("hidden"); $("#login-screen").classList.remove("hidden"); $("#login-form").reset(); $("#email").focus(); }

async function navigateTo(page) {
    if (!pages[page]) return;
    if (state.user?.mustChangePassword && page !== "settings") page = "settings";
    if (page === "firefighters" && state.user?.role !== "ADMIN") return;
    Object.values(pages).forEach((item) => item.classList.add("hidden")); pages[page].classList.remove("hidden");
    $$(".nav-item").forEach((item) => item.classList.toggle("active", item.dataset.page === page));
    $("#page-title").textContent = pageNames[page]; $("#breadcrumb").textContent = `Operação / ${pageNames[page]}`;
    const loaders = { dashboard: loadDashboard, schedules: loadSchedule, firefighters: loadFirefighters, unavailabilities: loadUnavailabilities, holidays: loadHolidays };
    if (loaders[page]) await loaders[page]();
}
async function restoreSession() { try { openDashboard(await apiRequest("/api/users/me")); } catch (error) { if (error.status !== 401) showToast(error.message, "error"); closeDashboard(); } }

// Feriados
async function loadHolidays() {
    const year = Number($("#holiday-year").value), feedback = $("#holiday-feedback");
    if (year < 2000 || year > 2100) { feedback.textContent = "Informe um ano válido."; return; }
    feedback.textContent = "Carregando feriados..."; feedback.classList.remove("hidden"); $("#holiday-table").classList.add("hidden");
    try { state.holidays = await apiRequest(`/api/holidays?year=${year}`); renderHolidays(); } catch (error) { feedback.textContent = error.message; }
}
function renderHolidays() {
    if (!state.holidays.length) { $("#holiday-feedback").textContent = "Nenhum feriado foi cadastrado para este ano."; $("#holiday-feedback").classList.remove("hidden"); $("#holiday-table").classList.add("hidden"); return; }
    $("#holiday-list").innerHTML = state.holidays.map((item) => `<div class="holiday-row"><span>${formatDate(item.date)}</span><strong>${escapeHtml(item.name)}</strong><span class="holiday-actions">${state.user.role === "ADMIN" ? `<button class="row-action" data-holiday-edit="${item.id}">Editar</button><button class="row-action danger-link" data-holiday-delete="${item.id}">Excluir</button>` : "Somente leitura"}</span></div>`).join("");
    $("#holiday-feedback").classList.add("hidden"); $("#holiday-table").classList.remove("hidden");
}
function openHoliday(item = null) {
    $("#holiday-form").reset(); clearErrors($("#holiday-form")); $("#holiday-id").value = item?.id ?? ""; $("#holiday-date").value = item?.date ?? ""; $("#holiday-name").value = item?.name ?? ""; $("#holiday-dialog-title").textContent = item ? "Editar feriado" : "Novo feriado"; $("#holiday-dialog").showModal();
}
async function saveHoliday(event) {
    event.preventDefault(); const id = $("#holiday-id").value, button = $("#save-holiday-button"); setLoading(button, true, "Salvando...");
    try { await apiRequest(id ? `/api/holidays/${id}` : "/api/holidays", { method: id ? "PUT" : "POST", body: JSON.stringify({ date: $("#holiday-date").value, name: $("#holiday-name").value }) }); $("#holiday-dialog").close(); $("#holiday-year").value = $("#holiday-date").value.substring(0, 4); await loadHolidays(); showToast(id ? "Feriado atualizado." : "Feriado cadastrado."); }
    catch (error) { showErrors($("#holiday-form"), error.fieldErrors); showToast(error.message, "error"); } finally { setLoading(button, false); }
}

// Bombeiros
async function loadFirefighters() {
    const feedback = $("#firefighter-feedback"); feedback.textContent = "Carregando bombeiros..."; feedback.classList.remove("hidden"); $("#firefighter-table").classList.add("hidden");
    try { state.firefighters = await apiRequest("/api/firefighters"); renderFirefighters(); } catch (error) { feedback.textContent = error.message; }
}
function renderFirefighters() {
    const query = $("#firefighter-search").value.toLowerCase();
    const list = state.firefighters.filter((item) => [item.name, item.email, item.registration].some((value) => value.toLowerCase().includes(query)));
    if (!list.length) { $("#firefighter-feedback").textContent = "Nenhum bombeiro encontrado."; $("#firefighter-feedback").classList.remove("hidden"); $("#firefighter-table").classList.add("hidden"); return; }
    $("#firefighter-list").innerHTML = list.map((item) => `<div class="data-row firefighter-row"><span class="person-cell"><i class="mini-avatar navy">${getInitials(item.name)}</i><span><strong>${escapeHtml(item.name)}</strong><small>${escapeHtml(item.email)}</small></span></span><span>${escapeHtml(item.registration)}</span><span>${escapeHtml(item.phone)}</span><span><i class="status ${item.active ? "approved" : "rejected"}">${item.active ? "Ativo" : "Inativo"}</i></span><span>${item.active ? `<button class="row-action danger-link" data-firefighter-deactivate="${item.firefighterId}">Desativar</button>` : "—"}</span></div>`).join("");
    $("#firefighter-feedback").classList.add("hidden"); $("#firefighter-table").classList.remove("hidden");
}
async function registerFirefighter(event) {
    event.preventDefault(); const form = $("#firefighter-form"), button = $('button[type="submit"]', form); setLoading(button, true, "Cadastrando...");
    try { const result = await apiRequest("/api/firefighters", { method: "POST", body: JSON.stringify({ name: $("#firefighter-name").value, email: $("#firefighter-email").value, registration: $("#firefighter-registration").value, phone: $("#firefighter-phone").value, temporaryPassword: $("#firefighter-temporary-password").value }) }); $("#firefighter-dialog").close(); await loadFirefighters(); showToast(`${result.name} foi cadastrado com senha temporária.`); }
    catch (error) { showErrors(form, error.fieldErrors); showToast(error.message, "error"); } finally { setLoading(button, false); }
}

// Indisponibilidades
async function loadUnavailabilities() {
    const admin = state.user.role === "ADMIN", feedback = $("#unavailability-feedback");
    $("#unavailability-page-title").textContent = admin ? "Análise de indisponibilidades" : "Minhas indisponibilidades"; $("#unavailability-list-title").textContent = admin ? "Solicitações pendentes" : "Minhas solicitações";
    feedback.textContent = "Carregando solicitações..."; feedback.classList.remove("hidden"); $("#unavailability-table").classList.add("hidden");
    try { state.unavailabilities = await apiRequest(admin ? "/api/unavailabilities/pending" : "/api/unavailabilities/me"); renderUnavailabilities(); } catch (error) { feedback.textContent = error.message; }
}
function renderUnavailabilities() {
    if (!state.unavailabilities.length) { $("#unavailability-feedback").textContent = "Nenhuma solicitação encontrada."; $("#unavailability-feedback").classList.remove("hidden"); $("#unavailability-table").classList.add("hidden"); return; }
    const admin = state.user.role === "ADMIN";
    $("#unavailability-list").innerHTML = state.unavailabilities.map((item) => `<div class="data-row unavailability-row"><span>${escapeHtml(item.firefighterName)}</span><span>${labels.types[item.type]}</span><span>${formatDate(item.startDate)} a ${formatDate(item.endDate)}</span><span><i class="status ${item.status.toLowerCase()}">${labels.statuses[item.status]}</i></span><span class="row-actions">${admin ? `<button class="row-action" data-unavailability-approve="${item.id}">Aprovar</button><button class="row-action danger-link" data-unavailability-reject="${item.id}">Rejeitar</button>` : "—"}</span></div>`).join("");
    $("#unavailability-feedback").classList.add("hidden"); $("#unavailability-table").classList.remove("hidden");
}
async function createUnavailability(event) {
    event.preventDefault(); const form = $("#unavailability-form"), button = $('button[type="submit"]', form); setLoading(button, true, "Enviando...");
    try { await apiRequest("/api/unavailabilities", { method: "POST", body: JSON.stringify({ type: $("#unavailability-type").value, startDate: $("#unavailability-start-date").value, endDate: $("#unavailability-end-date").value, reason: $("#unavailability-reason").value || null }) }); $("#unavailability-dialog").close(); await loadUnavailabilities(); showToast("Solicitação enviada."); }
    catch (error) { showErrors(form, error.fieldErrors); showToast(error.message, "error"); } finally { setLoading(button, false); }
}

// Escalas
const scheduleMonth = $("#schedule-month"), scheduleYear = $("#schedule-year");
scheduleMonth.innerHTML = monthNames.map((name, index) => `<option value="${index + 1}">${name}</option>`).join("");
async function loadSchedule() {
    const feedback = $("#schedule-feedback"); feedback.textContent = "Carregando escala..."; feedback.classList.remove("hidden"); $("#schedule-table").classList.add("hidden");
    try { state.schedule = await apiRequest(`/api/monthly-schedules/${Number(scheduleYear.value)}/${Number(scheduleMonth.value)}`); renderSchedule(); }
    catch (error) { state.schedule = null; feedback.textContent = error.status === 404 ? "Nenhuma escala encontrada para este período." : error.message; $("#schedule-summary").classList.add("hidden"); $("#publish-schedule-button").classList.add("hidden"); }
}
function renderSchedule() {
    const item = state.schedule;
    $("#schedule-summary").innerHTML = `<strong>${monthNames[item.month - 1]} de ${item.year}</strong><span class="status ${item.status === "PUBLISHED" ? "approved" : "pending"}">${labels.statuses[item.status]}</span><small>Criada em ${formatDateTime(item.createdAt)}</small>`;
    $("#schedule-summary").classList.remove("hidden"); $("#publish-schedule-button").classList.toggle("hidden", state.user.role !== "ADMIN" || item.status !== "DRAFT");
    $("#schedule-assignment-list").innerHTML = item.assignments.map((duty) => `<div class="data-row schedule-row"><span>${formatDate(duty.dutyDate)}</span><span>${labels.dayTypes[duty.dayType]}</span><span><strong>${escapeHtml(duty.firefighterName)}</strong><small>${escapeHtml(duty.firefighterRegistration)}</small></span><span>${formatDateTime(duty.startDateTime)}<br>${formatDateTime(duty.endDateTime)}</span><span>${state.user.role === "ADMIN" && item.status === "DRAFT" ? `<button class="row-action" data-reassign-date="${duty.dutyDate}">Remanejar</button>` : "—"}</span></div>`).join("");
    $("#schedule-feedback").classList.add("hidden"); $("#schedule-table").classList.remove("hidden");
}
async function generateSchedule() { const button = $("#generate-schedule-button"); setLoading(button, true, "Gerando..."); try { state.schedule = await apiRequest("/api/monthly-schedules", { method: "POST", body: JSON.stringify({ year: Number(scheduleYear.value), month: Number(scheduleMonth.value) }) }); renderSchedule(); showToast("Escala gerada."); } catch (error) { showToast(error.message, "error"); } finally { setLoading(button, false); } }
async function publishSchedule() { try { state.schedule = await apiRequest(`/api/monthly-schedules/${state.schedule.year}/${state.schedule.month}/publication`, { method: "POST" }); renderSchedule(); showToast("Escala publicada."); } catch (error) { showToast(error.message, "error"); } }
function openReassignment(date) { $("#reassignment-date").value = date; $("#reassignment-firefighter").innerHTML = `<option value="">Selecione</option>${state.firefighters.filter((item) => item.active).map((item) => `<option value="${item.firefighterId}">${escapeHtml(item.name)} — ${escapeHtml(item.registration)}</option>`).join("")}`; $("#reassignment-dialog").showModal(); }
async function reassign(event) { event.preventDefault(); try { await apiRequest(`/api/monthly-schedules/${state.schedule.year}/${state.schedule.month}/assignments/${$("#reassignment-date").value}`, { method: "PUT", body: JSON.stringify({ firefighterId: Number($("#reassignment-firefighter").value) }) }); $("#reassignment-dialog").close(); await loadSchedule(); showToast("Plantão remanejado."); } catch (error) { showToast(error.message, "error"); } }

// Dashboard
async function loadDashboard() {
    const today = new Date(); scheduleYear.value = today.getFullYear(); scheduleMonth.value = today.getMonth() + 1;
    await Promise.allSettled([loadDashboardSchedule(), state.user.role === "ADMIN" ? loadDashboardAdmin() : loadDashboardFirefighter()]);
}
async function loadDashboardSchedule() {
    try { const year = Number(scheduleYear.value), month = Number(scheduleMonth.value); const schedule = await apiRequest(`/api/monthly-schedules/${year}/${month}`); state.schedule = schedule; renderCalendar(schedule.assignments, year, month); renderDutyList(schedule.assignments); setText("#covered-duties-metric", `${schedule.assignments.length}/${new Date(year, month, 0).getDate()}`); setText("#schedule-status-metric", labels.statuses[schedule.status]); }
    catch { renderCalendar([], Number(scheduleYear.value), Number(scheduleMonth.value)); renderDutyList([]); }
}
async function loadDashboardAdmin() { const [firefighters, pending] = await Promise.all([apiRequest("/api/firefighters"), apiRequest("/api/unavailabilities/pending")]); state.firefighters = firefighters; state.unavailabilities = pending; setText("#active-firefighters-metric", firefighters.filter((item) => item.active).length); setText("#pending-unavailabilities-metric", pending.length); setText("#welcome-summary", pending.length ? `Há ${pending.length} solicitações aguardando sua análise.` : "Não há solicitações pendentes no momento."); setText("#dashboard-unavailability-title", "Indisponibilidades pendentes"); const badge = $("#pending-nav-badge"); badge.textContent = pending.length; badge.classList.toggle("hidden", !pending.length); renderDashboardRequests(pending.slice(0, 3)); }
async function loadDashboardFirefighter() { const mine = await apiRequest("/api/unavailabilities/me"); state.unavailabilities = mine; const pending = mine.filter((item) => item.status === "PENDING"); setText("#pending-unavailabilities-metric", pending.length); setText("#welcome-summary", pending.length ? `Você possui ${pending.length} solicitações aguardando análise.` : "Suas solicitações estão em dia."); setText("#dashboard-unavailability-title", "Minhas indisponibilidades"); renderDashboardRequests(mine.slice(0, 3)); }
function setText(selector, value) { const item = $(selector); if (item) item.textContent = value; }
function renderCurrentDate() {
    const currentDate = new Intl.DateTimeFormat("pt-BR", {
        weekday: "long",
        day: "2-digit",
        month: "long"
    }).format(new Date());

    setText("#dashboard-current-date", currentDate);
}
function renderDashboardRequests(items) { const container = $("#dashboard-unavailability-list"); if (!container) return; const heading = $(".request-heading", container)?.outerHTML ?? ""; container.innerHTML = heading + (items.length ? items.map((item) => `<div class="request-row"><span>${escapeHtml(item.firefighterName)}</span><span>${labels.types[item.type]}</span><span>${formatDate(item.startDate)} a ${formatDate(item.endDate)}</span><span><i class="status ${item.status.toLowerCase()}">${labels.statuses[item.status]}</i></span><span><button class="row-action" data-page="unavailabilities">Ver</button></span></div>`).join("") : `<div class="empty-inline">Nenhuma solicitação para exibir.</div>`); $$("[data-page]", container).forEach((button) => button.addEventListener("click", () => navigateTo(button.dataset.page))); }
function renderCalendar(assignments, year, month) { setText("#dashboard-calendar-title", `${monthNames[month - 1]} de ${year}`); const map = new Map(assignments.map((item) => [item.dutyDate, item])); const offset = new Date(year, month - 1, 1).getDay(), total = new Date(year, month, 0).getDate(), cells = Array(offset).fill(null).concat(Array.from({ length: total }, (_, index) => index + 1)); while (cells.length % 7) cells.push(null); $("#calendar-grid").innerHTML = cells.map((day, index) => { if (!day) return `<div class="calendar-day muted"></div>`; const key = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`, duty = map.get(key); return `<div class="calendar-day ${index % 7 === 0 || index % 7 === 6 ? "weekend" : ""}"><span class="day-number">${day}</span>${duty ? `<span class="day-duty">${escapeHtml(duty.firefighterName)}</span>` : ""}</div>`; }).join(""); }
function renderDutyList(assignments) { $("#duty-list").innerHTML = assignments.slice(0, 5).map((item) => `<article class="duty-item"><div class="duty-date"><strong>${item.dutyDate.substring(8)}</strong><span>${item.dutyDate.substring(5, 7)}</span></div><div class="duty-copy"><strong>${escapeHtml(item.firefighterName)}</strong><span>${escapeHtml(item.firefighterRegistration)}</span></div><span class="duty-type">24H</span></article>`).join("") || `<div class="empty-inline">Nenhuma escala neste mês.</div>`; }
async function shiftDashboardMonth(offset) { const date = new Date(Number(scheduleYear.value), Number(scheduleMonth.value) - 1 + offset, 1); scheduleYear.value = date.getFullYear(); scheduleMonth.value = date.getMonth() + 1; await loadDashboardSchedule(); }

async function changePassword(event) { event.preventDefault(); const form = event.currentTarget, button = $('button[type="submit"]', form); setLoading(button, true, "Atualizando..."); try { await apiRequest("/api/users/me/password", { method: "PUT", body: JSON.stringify({ currentPassword: $("#current-password").value, newPassword: $("#new-password").value, newPasswordConfirmation: $("#new-password-confirmation").value }) }); form.reset(); applyUser(await apiRequest("/api/users/me")); showToast("Senha atualizada."); navigateTo("dashboard"); } catch (error) { showErrors(form, error.fieldErrors); showToast(error.message, "error"); } finally { setLoading(button, false); } }
function confirmAction(title, message) { const dialog = $("#confirm-dialog"); $("#confirm-title").textContent = title; $("#confirm-message").textContent = message; dialog.showModal(); return new Promise((resolve) => dialog.addEventListener("close", () => resolve(dialog.returnValue === "confirm"), { once: true })); }

// Eventos
$("#login-form").addEventListener("submit", async (event) => { event.preventDefault(); const button = $('button[type="submit"]', event.currentTarget); setLoading(button, true, "Entrando..."); try { const user = await apiRequest("/api/auth/login", { method: "POST", body: JSON.stringify({ email: $("#email").value, password: $("#password").value }) }); openDashboard(user); showToast(`Bem-vindo, ${user.name}.`); } catch (error) { showToast(error.message, "error"); $("#password").select(); } finally { setLoading(button, false); } });
$("#password-toggle").addEventListener("click", () => $("#password").type = $("#password").type === "password" ? "text" : "password");
$("#logout-button").addEventListener("click", async () => { try { await apiRequest("/api/auth/logout", { method: "POST" }); closeDashboard(); } catch (error) { showToast(error.message, "error"); } });
$$("[data-page]").forEach((button) => button.addEventListener("click", () => navigateTo(button.dataset.page)));
$$('.app-dialog button[value="cancel"]').forEach((button) => button.addEventListener("click", (event) => { event.preventDefault(); button.closest("dialog").close("cancel"); }));
$("#load-holidays-button").addEventListener("click", loadHolidays); $("#new-holiday-button").addEventListener("click", () => openHoliday()); $("#holiday-form").addEventListener("submit", saveHoliday);
$("#holiday-list").addEventListener("click", async (event) => { const edit = event.target.dataset.holidayEdit, remove = event.target.dataset.holidayDelete; if (edit) openHoliday(state.holidays.find((item) => item.id === Number(edit))); if (remove && await confirmAction("Excluir feriado", "O feriado será removido permanentemente.")) { try { await apiRequest(`/api/holidays/${remove}`, { method: "DELETE" }); await loadHolidays(); showToast("Feriado excluído."); } catch (error) { showToast(error.message, "error"); } } });
$("#new-firefighter-button").addEventListener("click", () => { $("#firefighter-form").reset(); clearErrors($("#firefighter-form")); $("#firefighter-dialog").showModal(); }); $("#load-firefighters-button").addEventListener("click", loadFirefighters); $("#firefighter-search").addEventListener("input", renderFirefighters); $("#firefighter-form").addEventListener("submit", registerFirefighter);
$("#firefighter-list").addEventListener("click", async (event) => { const id = event.target.dataset.firefighterDeactivate; if (id && await confirmAction("Desativar bombeiro", "O bombeiro perderá o acesso ao sistema.")) { try { await apiRequest(`/api/firefighters/${id}/deactivation`, { method: "PATCH" }); await loadFirefighters(); showToast("Bombeiro desativado."); } catch (error) { showToast(error.message, "error"); } } });
$("#new-unavailability-button").addEventListener("click", () => { $("#unavailability-form").reset(); clearErrors($("#unavailability-form")); $("#unavailability-dialog").showModal(); }); $("#load-unavailabilities-button").addEventListener("click", loadUnavailabilities); $("#unavailability-form").addEventListener("submit", createUnavailability);
$("#unavailability-list").addEventListener("click", async (event) => { const approve = event.target.dataset.unavailabilityApprove, reject = event.target.dataset.unavailabilityReject, id = approve ?? reject; if (!id) return; try { await apiRequest(`/api/unavailabilities/${id}/${approve ? "approval" : "rejection"}`, { method: "PATCH" }); await loadUnavailabilities(); showToast(approve ? "Solicitação aprovada." : "Solicitação rejeitada."); } catch (error) { showToast(error.message, "error"); } });
$("#load-schedule-button").addEventListener("click", loadSchedule); $("#generate-schedule-button").addEventListener("click", generateSchedule); $("#publish-schedule-button").addEventListener("click", async () => { if (await confirmAction("Publicar escala", "Depois de publicada, a escala não poderá ser modificada.")) publishSchedule(); });
$("#schedule-assignment-list").addEventListener("click", async (event) => { const date = event.target.dataset.reassignDate; if (date) { if (!state.firefighters.length) await loadFirefighters(); openReassignment(date); } }); $("#reassignment-form").addEventListener("submit", reassign); $("#password-change-form").addEventListener("submit", changePassword); $("#generate-button").addEventListener("click", () => navigateTo("schedules"));
$("#dashboard-previous-month").addEventListener("click", () => shiftDashboardMonth(-1));
$("#dashboard-next-month").addEventListener("click", () => shiftDashboardMonth(1));
$("#dashboard-current-month").addEventListener("click", () => { const today = new Date(); scheduleYear.value = today.getFullYear(); scheduleMonth.value = today.getMonth() + 1; loadDashboardSchedule(); });

const now = new Date(); $("#holiday-year").value = now.getFullYear(); scheduleYear.value = now.getFullYear(); scheduleMonth.value = now.getMonth() + 1;
renderCurrentDate();
restoreSession();
