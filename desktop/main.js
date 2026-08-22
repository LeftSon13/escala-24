const {
    app,
    BrowserWindow,
    dialog,
    ipcMain
} = require("electron");

const {
    execFile
} = require("node:child_process");

const fs = require("node:fs");
const path = require("node:path");

const APP_URL = "http://localhost:3000";
const APP_ORIGIN = new URL(APP_URL).origin;
const DEVELOPMENT_COMPOSE_DIRECTORY =
    path.resolve(__dirname, "..");

function loadApplication(mainWindow) {
    mainWindow.loadURL(APP_URL);
}

function loadOfflineScreen(mainWindow) {
    mainWindow.loadFile(
        path.join(__dirname, "offline.html")
    );
}

function isValidComposeDirectory(directory) {
    return (
        fs.existsSync(
            path.join(directory, "docker-compose.yml")
        )
        && fs.existsSync(
            path.join(directory, ".env")
        )
    );
}

function getConfigurationFile() {
    return path.join(
        app.getPath("userData"),
        "desktop-config.json"
    );
}

function readSavedComposeDirectory() {
    try {
        const configuration = JSON.parse(
            fs.readFileSync(
                getConfigurationFile(),
                "utf8"
            )
        );

        if (
            isValidComposeDirectory(
                configuration.composeDirectory
            )
        ) {
            return configuration.composeDirectory;
        }
    } catch (error) {
        console.log(
            "Nenhuma pasta válida foi configurada anteriormente."
        );
    }

    return null;
}

function saveComposeDirectory(directory) {
    fs.writeFileSync(
        getConfigurationFile(),
        JSON.stringify(
            {
                composeDirectory: directory
            },
            null,
            2
        ),
        "utf8"
    );
}

async function selectComposeDirectory() {
    const selection = await dialog.showOpenDialog({
        title: "Selecione a pasta de instalação do Escala 24",
        properties: [
            "openDirectory"
        ]
    });

    if (selection.canceled) {
        return null;
    }

    const selectedDirectory = selection.filePaths[0];

    if (!isValidComposeDirectory(selectedDirectory)) {
        await dialog.showMessageBox({
            type: "warning",
            title: "Pasta inválida",
            message: "A pasta selecionada não contém docker-compose.yml e .env."
        });

        return null;
    }

    saveComposeDirectory(selectedDirectory);
    return selectedDirectory;
}

async function resolveComposeDirectory() {
    if (!app.isPackaged) {
        return DEVELOPMENT_COMPOSE_DIRECTORY;
    }

    const savedDirectory =
        readSavedComposeDirectory();

    if (savedDirectory) {
        return savedDirectory;
    }

    return selectComposeDirectory();
}

async function startLocalServices() {
    const composeDirectory =
        await resolveComposeDirectory();

    if (!composeDirectory) {
        return {
            success: false,
            message: "Selecione a pasta do Escala 24 para iniciar os serviços."
        };
    }

    return new Promise((resolve) => {
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
                cwd: composeDirectory,
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
