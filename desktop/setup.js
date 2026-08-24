const setupForm = document.querySelector("#setup-form");
const configureButton = document.querySelector("#configure-button");
const setupStatus = document.querySelector("#setup-status");

function showStatus(message, type = "") {
  setupStatus.textContent = message;
  setupStatus.className = "status";

  if (type) {
    setupStatus.classList.add(type);
  }
}

setupForm.addEventListener("submit", async (event) => {
  event.preventDefault();

  const adminName = document.querySelector("#admin-name").value.trim();

  const adminEmail = document.querySelector("#admin-email").value.trim();

  const adminPassword = document.querySelector("#admin-password").value;

  const passwordConfirmation = document.querySelector(
    "#admin-password-confirmation",
  ).value;

  if (adminPassword !== passwordConfirmation) {
    showStatus("A senha e a confirmação precisam ser iguais.", "error");
    return;
  }

  if (adminPassword.length < 12) {
    showStatus("A senha precisa possuir pelo menos 12 caracteres.", "error");
    return;
  }

  configureButton.disabled = true;

  showStatus("Preparando a instalação local do Escala 24...");

  try {
    const result = await window.desktop.configureInstallation({
      adminName,
      adminEmail,
      adminPassword,
    });

    showStatus(result.message, result.success ? "success" : "error");

    if (!result.success) {
      configureButton.disabled = false;
    }
  } catch (error) {
    console.error("Erro ao configurar a instalação:", error);

    showStatus("Ocorreu um erro inesperado durante a configuração.", "error");

    configureButton.disabled = false;
  }
});
