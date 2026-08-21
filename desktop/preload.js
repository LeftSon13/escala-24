const {
    contextBridge,
    ipcRenderer
} = require("electron");

contextBridge.exposeInMainWorld("desktop", {
    retryApplicationConnection() {
        ipcRenderer.send("retry-application-connection");
    }
});
