# 🎵 AURA MUSIC (Android)

<div align="center">

**Reproductor de música flotante, ultra-rápido y con estética Frosted Glass Cyberpunk para Android.**

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2%2B-purple?logo=kotlin&logoColor=white)](#)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](#)
[![Engine](https://img.shields.io/badge/Engine-AndroidX%20Media3%20(ExoPlayer)-red)](#)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-blue?logo=githubactions&logoColor=white)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## ✨ Características Principales

- 🎨 **Estética Studio Deck & Frosted Glass**: Interfaz construida en Jetpack Compose con desenfoques (*Frosted Glass*), disco de vinilo con física de giro interactiva, waveform animado y branding oficial de Aura Music.
- ⚡ **Motor de Audio Profesional (AndroidX Media3)**: Reproducción nativa en segundo plano con `MediaSessionService`, controles en la pantalla de bloqueo y barra de notificaciones, gestión inteligente de foco de audio y desconexión de auriculares (`becoming noisy`).
- 🎛️ **Ecualizador DSP de 5 Bandas**: Procesamiento digital por hardware con presets de estudio (*Cyberpunk, Synthwave, Bass Boost, Vocal Clarity, Flat*), realce de graves (*Bass Boost*) y sonido envolvente 3D.
- 🎤 **Letras Sincronizadas en Tiempo Real (Karaoke)**: Integración con motor LRCLIB para autoscroll y resaltado neón palabra por palabra de canciones.
- 📥 **Gestor de Descargas Offline**: Descargas multi-hilo con OkHttp, seguimiento de progreso porcentual y reproducción 100% sin conexión a internet.
- 📂 **Escáner de Almacenamiento Local**: Detección instantánea de archivos MP3, FLAC y WAV en el almacenamiento del dispositivo mediante `MediaStore`.
- 📋 **Gestión de Colecciones & Listas de Reproducción**: Creación y administración de playlists personalizadas respaldadas en SQLite (`AuraDatabase`).
- ⏱️ **Temporizador de Apagado (Sleep Timer)**: Modo de auto-standby programable (15, 30, 45, 60 min) o al finalizar la pista actual con desvanecimiento de volumen.
- ⚙️ **Consola de Configuración de Estudio**: Selector de calidad de audio (Hi-Res 320 kbps, 160 kbps, 96 kbps ahorro de datos), normalización de volumen y paletas de color.
- 🚀 **CI/CD Automatizado**: Flujo de trabajo en GitHub Actions para compilar el APK debug/release y publicar releases en cada tag.

---

## 🛠️ Stack Tecnológico

* **Lenguaje**: [Kotlin 2.2+](https://kotlinlang.org/)
* **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material 3 Expressive
* **Motor Multimedia**: [AndroidX Media3 1.5 (ExoPlayer & MediaSession)](https://developer.android.com/media/media3)
* **Arquitectura**: Clean Architecture + MVVM con StateFlow reactivo
* **Base de Datos**: SQLite nativo con transacciones optimizadas (`AuraDatabase`)
* **Red & Descargas**: OkHttp 4 + Coil Compose para carga y renderizado de carátulas
* **Efectos de Audio**: Android AudioFX (`Equalizer`, `BassBoost`, `Virtualizer`)

---

## 🚀 Compilación e Instalación

Para compilar el proyecto localmente con Android SDK:

```bash
# Clonar el repositorio
git clone git@github.com:respaldoperez348-rgb/AURA-MUSIC-ANDROID.git
cd AURA-MUSIC-ANDROID

# Compilar el APK de depuración
./gradlew assembleDebug

# Instalar en tu dispositivo o emulador vía ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más detalles.
