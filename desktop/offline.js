const startServicesButton =
    document.querySelector("#start-services-button");

const retryButton =
    document.querySelector("#retry-button");

const serviceStatus =
    document.querySelector("#service-status");

retryButton.addEventListener("click", () => {
    window.desktop.retryApplicationConnection();
});

startServicesButton.addEventListener("click", async () => {
    startServicesButton.disabled = true;
    retryButton.disabled = true;
    serviceStatus.textContent = "Iniciando os serviços locais...";

    try {
        const result =
            await window.desktop.startLocalServices();

        serviceStatus.textContent = result.message;

        if (result.success) {
            serviceStatus.textContent =
                `${result.message} Tentaremos conectar novamente em alguns segundos.`;

            setTimeout(() => {
                window.desktop.retryApplicationConnection();
            }, 5000);

            return;
        }
    } catch (error) {
        console.error(
            "Erro ao solicitar a inicialização dos serviços:",
            error
        );

        serviceStatus.textContent =
            "Ocorreu um erro inesperado ao iniciar os serviços.";
    }

    startServicesButton.disabled = false;
    retryButton.disabled = false;
});
