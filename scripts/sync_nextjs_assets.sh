#!/bin/bash
set -e

echo "=== Syncing Next.js Web App to Android Assets ==="

SOURCE_DIR="${1:-.}"
DEST_DIR="app/src/main/assets/www"

if [ -f "$SOURCE_DIR/package.json" ]; then
  echo "Building Next.js project in $SOURCE_DIR..."
  cd "$SOURCE_DIR"
  npm run build
  if [ -d "out" ]; then
    cd - > /dev/null
    mkdir -p "$DEST_DIR"
    cp -r "$SOURCE_DIR/out"/* "$DEST_DIR/"
    echo "✓ Successfully copied $SOURCE_DIR/out to $DEST_DIR"
  else
    echo "⚠ Error: 'out' folder not found. Ensure next.config.js has { output: 'export' }."
    exit 1
  fi
else
  echo "⚠ package.json not found in $SOURCE_DIR."
fi
