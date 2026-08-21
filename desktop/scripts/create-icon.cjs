const fs = require("node:fs/promises");
const path = require("node:path");
const pngToIco = require("png-to-ico").default;

const sourcePath = path.join(
    __dirname,
    "..",
    "assets",
    "icon-source.png"
);
const destinationPath = path.join(
    __dirname,
    "..",
    "assets",
    "icon.ico"
);

async function createIcon() {
    const iconBuffer = await pngToIco(sourcePath);

    await fs.writeFile(destinationPath, iconBuffer);

    console.log(`Ícone criado em: ${destinationPath}`);
}

createIcon().catch((error) => {
    console.error("Não foi possível criar o ícone do Windows.", error);
    process.exitCode = 1;
});
