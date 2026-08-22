const {
    app,
    BrowserWindow,
    ipcMain
} = require("electron");

const {
    execFile
} = require("node:child_process");

const path = require("node:path");

const APP_URL = "http://localhost:3000";
const APP_ORIGIN = new URL(APP_URL).origin;
const COMPOSE_DIRECTORY = path.resolve(__dirname, "..");

function loadApplication(mainWindow) {
    mainWindow.loadURL(APP_URL);
}

function loadOfflineScreen(mainWindow) {
    mainWindow.loadFile(
        path.join(__dirname, "offline.html")
    );
}

function startLocalServices() {
    return new Promise((resolve) => {
        if (app.isPackaged) {
            resolve({
                success: false,
                message: "A inicialização dos serviços pelo instalador ainda está em desenvolvimento."
            });
            return;
        }

        execFile(
            "docker",
            [
                "compose",
                "--project-name",
                "escala-24",
                "up",
                "-d"
],
            {
                cwd: COMPOSE_DIRECTORY,
                windowsHide: true
            },
            (error) => {
                if (error) {
                    console.error(
                        "Não foi possível iniciar os serviços:",
                        error
                    );

                    resolve({
                        success: false,
                        message: "Não foi possível iniciar os serviços. Confirme se o Docker Desktop está aberto."
                    });
                    return;
                }

                resolve({
                    success: true,
                    message: "Serviços iniciados. Aguarde enquanto o Escala 24 fica disponível."
                });
            }
        );
    });
}

function createWindow() {
    const mainWindow = new BrowserWindow({
        width: 1440,
        height: 900,
        minWidth: 1024,
        minHeight: 700,
        show: false,
        icon: path.join(
            __dirname,
            "assets",
            "icon.ico"
        ),
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
            sandbox: true,
            preload: path.join(
                __dirname,
                "preload.js"
            )
        }
    });

    mainWindow.removeMenu();

    mainWindow.once("ready-to-show", () => {
        mainWindow.show();
    });

    mainWindow.webContents.on(
        "did-fail-load",
        (
            event,
            errorCode,
            errorDescription,
            validatedUrl,
            isMainFrame
        ) => {
            if (
                isMainFrame
                && new URL(validatedUrl).origin === APP_ORIGIN
            ) {
                loadOfflineScreen(mainWindow);
            }
        }
    );

    loadApplication(mainWindow);
}

app.whenReady().then(() => {
    ipcMain.on(
        "retry-application-connection",
        (event) => {
            const mainWindow =
                BrowserWindow.fromWebContents(event.sender);

            if (mainWindow) {
                loadApplication(mainWindow);
            }
        }
    );

    ipcMain.handle(
        "start-local-services",
        () => startLocalServices()
    );

    createWindow();

    app.on("activate", () => {
        if (
            BrowserWindow.getAllWindows().length === 0
        ) {
            createWindow();
        }
    });
});

app.on("window-all-closed", () => {
    if (process.platform !== "darwin") {
        app.quit();
    }
});
