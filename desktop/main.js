const {
    app,
    BrowserWindow,
    ipcMain
} = require("electron");

const path = require("node:path");

const APP_URL = "http://localhost:3000";
const APP_ORIGIN = new URL(APP_URL).origin;

function loadApplication(mainWindow) {
    mainWindow.loadURL(APP_URL);
}

function loadOfflineScreen(mainWindow) {
    mainWindow.loadFile(path.join(__dirname, "offline.html"));
}

function createWindow() {
    const mainWindow = new BrowserWindow({
        width: 1440,
        height: 900,
        minWidth: 1024,
        minHeight: 700,
        show: false,
        icon: path.join(__dirname, "assets", "icon.ico"),
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
            sandbox: true,
            preload: path.join(__dirname, "preload.js")
        }
    });

    mainWindow.removeMenu();

    mainWindow.once("ready-to-show", () => {
        mainWindow.show();
    });

    mainWindow.webContents.on(
        "did-fail-load",
        (event, errorCode, errorDescription, validatedUrl, isMainFrame) => {
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
    ipcMain.on("retry-application-connection", (event) => {
        loadApplication(BrowserWindow.fromWebContents(event.sender));
    });

    createWindow();

    app.on("activate", () => {
        if (BrowserWindow.getAllWindows().length === 0) {
            createWindow();
        }
    });
});

app.on("window-all-closed", () => {
    if (process.platform !== "darwin") {
        app.quit();
    }
});
