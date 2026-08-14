const loginScreen = document.querySelector("#login-screen");
const loginForm = document.querySelector("#login-form");
const emailInput = document.querySelector("#email");
const loginButton = loginForm.querySelector(
    'button[type="submit"]'
);
const appShell = document.querySelector("#app-shell");
const passwordInput = document.querySelector("#password");
const passwordToggle = document.querySelector("#password-toggle");
const logoutButton = document.querySelector("#logout-button");
const pageTitle = document.querySelector("#page-title");
const dashboardPage = document.querySelector("#dashboard-page");
const placeholderPage = document.querySelector("#placeholder-page");
const placeholderTitle = document.querySelector("#placeholder-title");
const calendarGrid = document.querySelector("#calendar-grid");
const dutyList = document.querySelector("#duty-list");
const toast = document.querySelector("#toast");
const toastMessage = document.querySelector("#toast-message");
const profileName = document.querySelector(
    ".profile-copy strong"
);

const profileRole = document.querySelector(
    ".profile-copy span"
);

const profileAvatar = document.querySelector(
    ".sidebar-profile .avatar"
);

const welcomeTitle = document.querySelector(
    ".welcome-banner h2"
);

async function apiRequest(path, options = {}) {
    const response = await fetch(path, {
        credentials: "same-origin",
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...options.headers
        }
    });

    if (response.status === 204) {
        return null;
    }

    const responseBody = await response.json();

    if (!response.ok) {
        const error = new Error(
            responseBody.message ??
            "Não foi possível concluir a operação."
        );

        error.status = response.status;
        error.fieldErrors = responseBody.fieldErrors ?? {};

        throw error;
    }

    return responseBody;
}

let authenticatedUser = null;

const pageNames = {
    dashboard: "Visão geral",
    schedules: "Escalas mensais",
    firefighters: "Bombeiros",
    unavailabilities: "Indisponibilidades",
    holidays: "Feriados",
    settings: "Configurações"
};

const firefighterNames = [
    "Rafael Martins",
    "Camila Souza",
    "Diego Ferreira",
    "Marina Costa",
    "Lucas Almeida",
    "Ana Ribeiro",
    "Pedro Henrique"
];

const nextDuties = [
    {
        day: "10",
        weekday: "DOM",
        name: "Rafael Martins",
        registration: "REG-014"
    },
    {
        day: "11",
        weekday: "SEG",
        name: "Camila Souza",
        registration: "REG-008"
    },
    {
        day: "12",
        weekday: "TER",
        name: "Diego Ferreira",
        registration: "REG-021"
    },
    {
        day: "13",
        weekday: "QUA",
        name: "Marina Costa",
        registration: "REG-005"
    }
];

function getInitials(name) {
    return name
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map((part) => part.charAt(0))
        .join("")
        .toUpperCase();
}

function getRoleName(role) {
    const roleNames = {
        ADMIN: "Administrador",
        FIREFIGHTER: "Bombeiro"
    };

    return roleNames[role] ?? role;
}

function applyAuthenticatedUser(user) {
    authenticatedUser = user;

    const firstName = user.name
        .trim()
        .split(/\s+/)[0];

    profileName.textContent = user.name;
    profileRole.textContent = getRoleName(user.role);
    profileAvatar.textContent = getInitials(user.name);
    welcomeTitle.textContent = `Bom dia, ${firstName}.`;
}

function openDashboard(user) {
    applyAuthenticatedUser(user);

    loginScreen.classList.add("hidden");
    appShell.classList.remove("hidden");

    renderCalendar();
    renderDutyList();
}

function closeDashboard() {
    authenticatedUser = null;

    appShell.classList.add("hidden");
    loginScreen.classList.remove("hidden");

    loginForm.reset();
    emailInput.focus();
}

function navigateTo(page) {
    const selectedPage = pageNames[page] ?? "Módulo";

    document.querySelectorAll(".nav-item").forEach((item) => {
        item.classList.toggle(
            "active",
            item.dataset.page === page
        );
    });

    pageTitle.textContent = selectedPage;

    if (page === "dashboard") {
        dashboardPage.classList.remove("hidden");
        placeholderPage.classList.add("hidden");
        return;
    }

    dashboardPage.classList.add("hidden");
    placeholderPage.classList.remove("hidden");
    placeholderTitle.textContent = selectedPage;
}

function renderCalendar() {
    const previousMonthDays = [26, 27, 28, 29, 30, 31];
    const currentMonthDays = Array.from(
        { length: 31 },
        (_, index) => index + 1
    );
    const nextMonthDays = [1, 2, 3, 4, 5];

    const days = [
        ...previousMonthDays.map((day) => ({
            day,
            muted: true
        })),
        ...currentMonthDays.map((day) => ({
            day,
            muted: false
        })),
        ...nextMonthDays.map((day) => ({
            day,
            muted: true
        }))
    ];

    calendarGrid.innerHTML = days
        .map((entry, index) => {
            const weekdayIndex = index % 7;
            const isWeekend =
                weekdayIndex === 0 || weekdayIndex === 6;
            const isToday = !entry.muted && entry.day === 10;
            const isHoliday = !entry.muted && entry.day === 15;

            const classes = [
                "calendar-day",
                entry.muted ? "muted" : "",
                isWeekend ? "weekend" : "",
                isToday ? "today" : "",
                isHoliday ? "holiday" : ""
            ]
                .filter(Boolean)
                .join(" ");

            let dutyText = "";

            if (!entry.muted) {
                const firefighter =
                    firefighterNames[
                    (entry.day - 1) %
                    firefighterNames.length
                    ];

                dutyText = isHoliday
                    ? "Feriado municipal"
                    : firefighter;
            }

            return `
                <div class="${classes}">
                    <span class="day-number">${entry.day}</span>
                    ${dutyText
                    ? `<span class="day-duty">${dutyText}</span>`
                    : ""
                }
                </div>
            `;
        })
        .join("");
}

function renderDutyList() {
    dutyList.innerHTML = nextDuties
        .map((duty) => `
            <article class="duty-item">
                <div class="duty-date">
                    <strong>${duty.day}</strong>
                    <span>${duty.weekday}</span>
                </div>

                <div class="duty-copy">
                    <strong>${duty.name}</strong>
                    <span>${duty.registration} · 08:00–08:00</span>
                </div>

                <span class="duty-type">24H</span>
            </article>
        `)
        .join("");
}

let toastTimeout;

function showToast(message) {
    toastMessage.textContent = message;
    toast.classList.remove("hidden");

    window.clearTimeout(toastTimeout);

    toastTimeout = window.setTimeout(() => {
        toast.classList.add("hidden");
    }, 3200);
}

async function restoreSession() {
    try {
        const user = await apiRequest("/api/users/me");

        openDashboard(user);
    } catch (error) {
        closeDashboard();

        if (error.status !== 401) {
            showToast(error.message);
        }
    }
}

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    loginButton.disabled = true;
    loginButton.textContent = "Entrando...";

    try {
        const user = await apiRequest(
            "/api/auth/login",
            {
                method: "POST",
                body: JSON.stringify({
                    email: emailInput.value,
                    password: passwordInput.value
                })
            }
        );

        openDashboard(user);
        showToast(`Bem-vindo, ${user.name}.`);
    } catch (error) {
        showToast(error.message);
        passwordInput.focus();
        passwordInput.select();
    } finally {
        loginButton.disabled = false;
        loginButton.innerHTML = `
            Acessar painel
            <span>→</span>
        `;
    }
});

passwordToggle.addEventListener("click", () => {
    const showingPassword =
        passwordInput.type === "text";

    passwordInput.type =
        showingPassword ? "password" : "text";

    passwordToggle.setAttribute(
        "aria-label",
        showingPassword
            ? "Mostrar senha"
            : "Ocultar senha"
    );
});

logoutButton.addEventListener("click", async () => {
    logoutButton.disabled = true;
    logoutButton.textContent = "Saindo...";

    try {
        await apiRequest("/api/auth/logout", {
            method: "POST"
        });

        closeDashboard();
    } catch (error) {
        showToast(error.message);
    } finally {
        logoutButton.disabled = false;
        logoutButton.textContent = "Sair";
    }
});

document.querySelectorAll("[data-page]").forEach((button) => {
    button.addEventListener("click", () => {
        navigateTo(button.dataset.page);
    });
});

document
    .querySelector("#generate-button")
    .addEventListener("click", () => {
        showToast(
            "A geração da escala será conectada à API na próxima etapa."
        );
    });

document.querySelectorAll(".row-action").forEach((button) => {
    button.addEventListener("click", () => {
        showToast(
            "Solicitação aberta para análise em modo demonstração."
        );
    });
});

restoreSession();