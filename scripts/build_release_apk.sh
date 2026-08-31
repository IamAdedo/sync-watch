#!/bin/bash
set -e

echo "===================================================="
echo " Next.js Capacitor App - Automated Android Build"
echo "===================================================="

# Check if Next.js export exists or needs building
if [ -f "package.json" ]; then
  echo "==> Building Next.js static export..."
  npm run build
  if [ -d "out" ]; then
    mkdir -p app/src/main/assets/www
    cp -r out/* app/src/main/assets/www/
    echo "==> Synced out/ -> app/src/main/assets/www/"
  fi
fi

echo "==> Running Gradle Unit Tests..."
gradle testDebugUnitTest

echo "==> Compiling Debug APK..."
gradle assembleDebug

echo "==> Compiling Release APK..."
gradle assembleRelease || echo "Release build requires keystore configuration."

echo "===================================================="
echo " Build Completed Successfully!"
echo " Debug APK: app/build/outputs/apk/debug/app-debug.apk"
echo "===================================================="
