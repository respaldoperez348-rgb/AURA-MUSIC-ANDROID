# AURA Music (Android) — v3.1.0

<div align="center">

### **Reproductor de música moderno, rápido y personalizable para Android**
*Streaming instantáneo con motor híbrido (`VISIONOS` + `InnerTubeX` + `Media3 SimpleCache`), 5 estilos de reproductor, Crossfade Dual-Deck, descargas offline con alerta sonora, letras sincronizadas con triple respaldo y diseño expresivo en Jetpack Compose.*

[![Version](https://img.shields.io/badge/Release-v3.1.0-F5A623?style=for-the-badge&logo=android&logoColor=black)](https://github.com/respaldoperez348-rgb/AURA-MUSIC-ANDROID/releases/tag/v3.1.0)
[![Platform](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-10B981?style=for-the-badge&logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](#)
[![Jetpack Compose](https://img.shields.io/badge/UI-Material%203%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](#)
[![Media3](https://img.shields.io/badge/Audio-AndroidX%20Media3%20ExoPlayer-EC4899?style=for-the-badge)](#)
[![License](https://img.shields.io/badge/Licencia-GPL%20v3.0-F59E0B?style=for-the-badge)](#)

**Creado por David Tineo**

<br/>

[**Descargar APK Release v3.1.0 (Recomendado - 20 MB)**](releases/AuraMusic-v3.1.0-Release.apk) &nbsp;•&nbsp; [**Descargar APK Debug v3.1.0 (29 MB)**](releases/AuraMusic-v3.1.0-Debug.apk) &nbsp;•&nbsp; [**Ver Lanzamiento Oficial (GitHub Releases)**](https://github.com/respaldoperez348-rgb/AURA-MUSIC-ANDROID/releases)

</div>

---

## 📸 Capturas de Pantalla (v3.1.0)

<div align="center">

<table>
  <tr>
    <td align="center" width="33%">
      <img src="screenshots/01_home_dashboard.png" width="100%" alt="Inicio Bento y Onda Acústica"/>
      <br/><br/>
      <b>01. Inicio Bento & Onda Acústica</b>
      <br/>
      <sub>Hero card con onda en vivo, mosaico Bento, historial reciente y más escuchados</sub>
    </td>
    <td align="center" width="33%">
      <img src="screenshots/02_explore_caracol.png" width="100%" alt="Explore en Espiral / Caracol"/>
      <br/><br/>
      <b>02. Explore en Caracol Dinámico</b>
      <br/>
      <sub>Recomendaciones en espiral adaptadas al género de lo que escuchas a 120 Hz</sub>
    </td>
    <td align="center" width="33%">
      <img src="screenshots/04_buscador_bento.png" width="100%" alt="Buscador Bento y Artistas"/>
      <br/><br/>
      <b>03. Buscador & Explorador Bento</b>
      <br/>
      <sub>Tarjetas de géneros con carátulas inclinadas, tendencias y artistas destacados</sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="33%">
      <img src="screenshots/05_player_studio_halo.png" width="100%" alt="Reproductor Studio Halo"/>
      <br/><br/>
      <b>04. Estilo Studio Halo + Live-Lyrics</b>
      <br/>
      <sub>Onda acústica interactiva, insignia OPUS kbps y letras sincronizadas en vivo</sub>
    </td>
    <td align="center" width="33%">
      <img src="screenshots/08_player_vinilo.png" width="100%" alt="Estilo Tocadiscos Vinilo"/>
      <br/><br/>
      <b>05. Estilo Tocadiscos Vinilo</b>
      <br/>
      <sub>Vinilo giratorio con brazo de aguja, controles laterales y vista previa de siguiente canción</sub>
    </td>
    <td align="center" width="33%">
      <img src="screenshots/10_player_reloj_gigante.png" width="100%" alt="Estilo Letras y Reloj Gigante"/>
      <br/><br/>
      <b>06. Estilo Letras & Reloj Ambiente</b>
      <br/>
      <sub>Panel dual deslizable entre letras sincronizadas en vivo y reloj digital gigante</sub>
    </td>
  </tr>
</table>

</div>

---

## ✨ Características Principales

- **Motor de Audio AndroidX Media3 (`ExoPlayer` + `MediaSessionService`)**:
  - Reproducción en segundo plano con controles nativos en el **Centro de Notificaciones** y pantalla de bloqueo.
  - **Auto-Radio (Reproducción continua)** inteligente que encola pistas relacionadas al finalizar la cola.
  - Resolución multi-cliente de flujos de audio (`IOS`, `ANDROID_VR`, `TVHTML5_SIMPLY_EMBEDDED_PLAYER`) con renovación automática de URLs expiradas.
- **Descubrimiento y Búsqueda en Tiempo Real (`WEB_REMIX` / `:innertube`)**:
  - **Inicio**: Selección rápida, *Tus más escuchados* (historial real), Artistas populares, Álbumes destacados y Tendencias globales.
  - **Explore en Caracol**: Curva espiral interactiva que se actualiza automáticamente según la canción o género que estés reproduciendo (*Para ti*, *Pop*, *Rock*, *Urbano*, *Indie*, *R&B*, *Electrónica*, *Clásicos*).
  - **Buscador con Sugerencias**: Autocompletado en tiempo real, filtros por Canciones/Artistas/Álbumes e indicador directo cuando una canción ya está en tus Favoritos.
- **Perfiles Completos de Artistas y Álbumes**:
  - Cabecera inmersiva de artista con insignia verificada, canciones populares, cuadrícula de álbumes, sencillos y pestaña **Sobre** con biografía, datos del artista y artistas relacionados interactivos.
- **Letras Sincronizadas (LRC)**:
  - Resaltado línea por línea sincronizado con el tiempo de reproducción y ocultamiento automático de la caja de controles para lectura a pantalla completa.
- **Descargas Offline con Notificaciones del Sistema**:
  - Descarga real a almacenamiento local con barra de progreso en el centro de notificaciones de Android y gestión directa (*Descargar* / *Eliminar descarga*).
- **Personalización Total**:
  - **7 Temas**: *Claro*, *Oscuro Estudio*, *Negro Puro AMOLED*, *Noche Violeta*, *Carbono Marino*, *Café Nocturno* y *Automático*.
  - **12 Colores de acento** y **6 Iconos dinámicos** para la pantalla de inicio del teléfono.

---

## 🏗️ Arquitectura del Proyecto

```text
Reproductor/
├── app/                  # Aplicación principal Android (Jetpack Compose + Media3 + SQLite)
│   └── src/main/kotlin/com/auramusic/app/
│       ├── data/         # Modelos, base de datos SQLite (UserManager) y MusicRepository
│       ├── playback/     # PlayerManager y AuraMusicService (MediaSessionService)
│       └── ui/           # Pantallas Compose, barra flotante segmentada, temas y componentes
├── innertube/            # Módulo Kotlin puro para cliente YouTube Music / Innertube (Ktor + Serialization)
├── releases/             # Paquetes instalables oficiales de AURA Music v3.0.0 (Release & Debug)
└── screenshots/          # Capturas oficiales de AURA Music v3.0.0
```

---

## 🚀 Compilación desde Código Fuente

1. Clona el repositorio:
   ```bash
   git clone git@github.com:respaldoperez348-rgb/AURA-MUSIC-ANDROID.git
   cd AURA-MUSIC-ANDROID
   ```
2. Compila el APK con Gradle:
   ```bash
   ./gradlew assembleRelease
   ```
3. El APK generado estará disponible en:
   `app/build/outputs/apk/release/app-release.apk`

---

## 👤 Autor y Licencia

- **Creador:** David Tineo
- **Repositorio Oficial:** [github.com/respaldoperez348-rgb/AURA-MUSIC-ANDROID](https://github.com/respaldoperez348-rgb/AURA-MUSIC-ANDROID)
- **Licencia:** Distribuido bajo la licencia **GNU General Public License v3.0 (GPL-3.0)**.
