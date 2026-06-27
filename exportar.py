import os

PROYECTO = "/home/javier/Descargas/facturacion"
SALIDA = "/home/javier/Descargas/facturacion_codigo.txt"

EXTENSIONES = {
    ".java",
    ".html",
    ".css",
    ".js",
    ".properties",
    ".yml",
    ".yaml",
    ".sql",
    ".xml"
}

# Archivos o librerías a ignorar
IGNORAR = {
    "bootstrap",
    "jquery",
    "popper",
    "fontawesome",
    "sweetalert"
}

with open(SALIDA, "w", encoding="utf-8") as salida:
    for root, dirs, files in os.walk(PROYECTO):

        dirs[:] = [
            d for d in dirs
            if d not in {"target", ".git", ".idea", ".vscode", "node_modules"}
        ]

        for archivo in files:

            if not any(archivo.endswith(ext) for ext in EXTENSIONES):
                continue

            nombre = archivo.lower()

            # Saltar librerías externas
            if any(lib in nombre for lib in IGNORAR):
                continue

            ruta = os.path.join(root, archivo)

            salida.write("\n" + "=" * 100 + "\n")
            salida.write(f"ARCHIVO: {os.path.relpath(ruta, PROYECTO)}\n")
            salida.write("=" * 100 + "\n\n")

            try:
                with open(ruta, "r", encoding="utf-8") as f:
                    salida.write(f.read())
            except Exception as e:
                salida.write(f"ERROR: {e}")

            salida.write("\n\n")

print(f"Exportado en: {SALIDA}")