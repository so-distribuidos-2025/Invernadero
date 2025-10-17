#!/bin/bash
# ============================================
# build_all_dockers.sh
# Construye todas las imágenes Docker encontradas
# en la carpeta ./components y sus subcarpetas.
# Cada imagen se etiqueta como invernadero-<componente>:1.0
# ============================================

set -e  # Detiene el script si ocurre un error

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPONENTS_DIR="$ROOT_DIR/components"

echo "🚀 Buscando Dockerfiles en: $COMPONENTS_DIR"
echo

# Busca todos los archivos llamados DockerFile (sin distinguir mayúsculas/minúsculas)
find "$COMPONENTS_DIR" -type f -iname "DockerFile" | while read -r DOCKERFILE; do
    DIR="$(dirname "$DOCKERFILE")"
    COMPONENT_NAME="$(basename "$DIR")"
    IMAGE_NAME="${COMPONENT_NAME,,}:1.0"  # imagen con tag 1.0

    echo "🛠️  Construyendo imagen para componente: $COMPONENT_NAME"
    echo "📦  Nombre de la imagen: $IMAGE_NAME"
    echo "📂  Ruta del Dockerfile: $DOCKERFILE"
    echo

    # Construcción de la imagen
    docker build -t "$IMAGE_NAME" -f "$DOCKERFILE" "$DIR"

    echo "✅ Imagen construida correctamente: $IMAGE_NAME"
    echo "-------------------------------------------"
    echo
done

echo "🎉 Todas las imágenes Docker se construyeron correctamente (tag: 1.0)."
