const { app, BrowserWindow, ipcMain } = require("electron");

const { execFile } = require("node:child_process");

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");

function resolveDockerExecutable() {
  const candidates = [
    process.env.LOCALAPPDATA
      ? path.join(
          process.env.LOCALAPPDATA,
          "Programs",
          "DockerDesktop",
          "resources",
          "bin",
          "docker.exe",
        )
      : null,
    process.env.PROGRAMFILES
      ? path.join(
          process.env.PROGRAMFILES,
          "Docker",
          "Docker",
          "resources",
          "bin",
          "docker.exe",
        )
      : null,
  ].filter(Boolean);

  return candidates.find((candidate) => fs.existsSync(candidate)) || "docker";
}

const userDataOverride = process.env.ESCALA24_DESKTOP_USER_DATA;

if (userDataOverride) {
  const isolatedUserDataDirectory = path.resolve(userDataOverride);

  fs.mkdirSync(isolatedUserDataDirectory, {
    recursive: true,
  });

  app.setPath("userData", isolatedUserDataDirectory);
}

const composeProjectName =
    process.env.ESCALA24_DESKTOP_PROJECT_NAME
    || "escala-24";

const APP_URL = "http://localhost:3000";
const APP_ORIGIN = new URL(APP_URL).origin;

const DEVELOPMENT_COMPOSE_DIRECTORY = path.resolve(__dirname, "..");

function loadApplication(mainWindow) {
  mainWindow.loadURL(APP_URL);
}

function loadOfflineScreen(mainWindow) {
  mainWindow.loadFile(path.join(__dirname, "offline.html"));
}

function loadSetupScreen(mainWindow) {
  mainWindow.loadFile(path.join(__dirname, "setup.html"));
}

function isValidComposeDirectory(directory) {
  return (
    typeof directory === "string" &&
    fs.existsSync(path.join(directory, "docker-compose.yml")) &&
    fs.existsSync(path.join(directory, ".env"))
  );
}

function getConfigurationFile() {
  return path.join(app.getPath("userData"), "desktop-config.json");
}

function getDeploymentDirectory() {
  return path.join(app.getPath("userData"), "deployment");
}

function getDeploymentResourcesDirectory() {
  if (app.isPackaged) {
    return path.join(process.resourcesPath, "deployment");
  }

  return path.join(__dirname, "deployment");
}

function readSavedComposeDirectory() {
  try {
    const configuration = JSON.parse(
      fs.readFileSync(getConfigurationFile(), "utf8"),
    );

    if (isValidComposeDirectory(configuration.composeDirectory)) {
      return configuration.composeDirectory;
    }
  } catch (error) {
    console.log("Nenhuma instalação local válida foi configurada.");
  }

  return null;
}

function saveComposeDirectory(directory) {
  fs.mkdirSync(app.getPath("userData"), {
    recursive: true,
  });

  fs.writeFileSync(
    getConfigurationFile(),
    JSON.stringify(
      {
        composeDirectory: directory,
      },
      null,
      2,
    ),
    "utf8",
  );
}

function validateSetupConfiguration(configuration) {
  if (!configuration || typeof configuration !== "object") {
    return "Os dados da configuração são inválidos.";
  }

  const { adminName, adminEmail, adminPassword } = configuration;

  if (typeof adminName !== "string" || adminName.trim().length < 3) {
    return "Informe um nome com pelo menos 3 caracteres.";
  }

  if (
    typeof adminEmail !== "string" ||
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(adminEmail.trim())
  ) {
    return "Informe um endereço de e-mail válido.";
  }

  if (typeof adminPassword !== "string" || adminPassword.length < 12) {
    return "A senha precisa possuir pelo menos 12 caracteres.";
  }

  const values = [adminName, adminEmail, adminPassword];

  if (values.some((value) => /[\r\n]/.test(value))) {
    return "Os campos não podem conter quebras de linha.";
  }

  return null;
}

function quoteEnvironmentValue(value) {
  return `'${value.replaceAll("\\", "\\\\").replaceAll("'", "\\'")}'`;
}

function createEnvironmentFile(configuration) {
  const databasePassword = crypto.randomBytes(32).toString("hex");

  const environmentVariables = [
    `ESCALA24_VERSION=${app.getVersion()}`,
    "POSTGRES_DB=escala24",
    "POSTGRES_USER=escala24_user",
    `POSTGRES_PASSWORD=${databasePassword}`,
    "ESCALA24_INITIAL_ADMIN_ENABLED=true",
    `ESCALA24_INITIAL_ADMIN_NAME=${quoteEnvironmentValue(
      configuration.adminName.trim(),
    )}`,
    `ESCALA24_INITIAL_ADMIN_EMAIL=${quoteEnvironmentValue(
      configuration.adminEmail.trim().toLowerCase(),
    )}`,
    `ESCALA24_INITIAL_ADMIN_PASSWORD=${quoteEnvironmentValue(
      configuration.adminPassword,
    )}`,
  ];

  return `${environmentVariables.join("\n")}\n`;
}

function prepareDeploymentDirectory(configuration) {
  const resourceDirectory = getDeploymentResourcesDirectory();

  const sourceComposeFile = path.join(resourceDirectory, "docker-compose.yml");

  if (!fs.existsSync(sourceComposeFile)) {
    throw new Error("O pacote não contém o arquivo docker-compose.yml.");
  }

  const deploymentDirectory = getDeploymentDirectory();

  fs.mkdirSync(deploymentDirectory, {
    recursive: true,
  });

  fs.copyFileSync(
    sourceComposeFile,
    path.join(deploymentDirectory, "docker-compose.yml"),
  );

  fs.writeFileSync(
    path.join(deploymentDirectory, ".env"),
    createEnvironmentFile(configuration),
    {
      encoding: "utf8",
      flag: "wx",
    },
  );

  saveComposeDirectory(deploymentDirectory);

  return deploymentDirectory;
}

async function resolveComposeDirectory() {
  if (!app.isPackaged) {
    return DEVELOPMENT_COMPOSE_DIRECTORY;
  }

  return readSavedComposeDirectory();
}

function runDockerCompose(composeDirectory) {
  return new Promise((resolve) => {
    execFile(
      resolveDockerExecutable(),
      [
        "compose",
        "--project-name",
        composeProjectName,
        "up",
        "-d",
      ],
      {
        cwd: composeDirectory,
        windowsHide: true,
      },
      (error) => {
        if (error) {
          console.error("Não foi possível iniciar os serviços:", error);

          resolve({
            success: false,
            message:
              "Não foi possível iniciar os serviços. Confirme se o Docker Desktop está aberto.",
          });
          return;
        }

        resolve({
          success: true,
          message: "Configuração concluída. O Escala 24 está sendo iniciado.",
        });
      },
    );
  });
}

async function startLocalServices() {
  const composeDirectory = await resolveComposeDirectory();

  if (!composeDirectory) {
    return {
      success: false,
      message: "A instalação local ainda não foi configurada.",
    };
  }

  return runDockerCompose(composeDirectory);
}

async function configureInstallation(configuration) {
  const validationError = validateSetupConfiguration(configuration);

  if (validationError) {
    return {
      success: false,
      message: validationError,
    };
  }

  try {
    const existingDirectory = readSavedComposeDirectory();

    if (existingDirectory) {
      return runDockerCompose(existingDirectory);
    }

    const deploymentDirectory = prepareDeploymentDirectory(configuration);

    return runDockerCompose(deploymentDirectory);
  } catch (error) {
    console.error("Não foi possível configurar a instalação:", error);

    return {
      success: false,
      message: "Não foi possível preparar a instalação local do Escala 24.",
    };
  }
}

function shouldShowSetupScreen() {
  return app.isPackaged && !readSavedComposeDirectory();
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
      preload: path.join(__dirname, "preload.js"),
    },
  });

  mainWindow.removeMenu();

  mainWindow.once("ready-to-show", () => {
    mainWindow.show();
  });

  mainWindow.webContents.on(
    "did-fail-load",
    (event, errorCode, errorDescription, validatedUrl, isMainFrame) => {
      if (isMainFrame && validatedUrl.startsWith(APP_ORIGIN)) {
        loadOfflineScreen(mainWindow);
      }
    },
  );

  if (shouldShowSetupScreen()) {
    loadSetupScreen(mainWindow);
    return;
  }

  loadApplication(mainWindow);
}

app.whenReady().then(() => {
  ipcMain.on("retry-application-connection", (event) => {
    const mainWindow = BrowserWindow.fromWebContents(event.sender);

    if (mainWindow) {
      loadApplication(mainWindow);
    }
  });

  ipcMain.handle("start-local-services", () => startLocalServices());

  ipcMain.handle("configure-installation", async (event, configuration) => {
    const result = await configureInstallation(configuration);

    if (result.success) {
      const mainWindow = BrowserWindow.fromWebContents(event.sender);

      if (mainWindow) {
        setTimeout(() => {
          loadApplication(mainWindow);
        }, 5000);
      }
    }

    return result;
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
