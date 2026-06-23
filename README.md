<div align="center">
  <img src="app/src/main/res/drawable/app_icon.png" width="200" height="200" alt="Patatatube Logo">
  <h1>🥔 Patatatube Android 🎧</h1>
  <p><b>¡Descarga cualquier vídeo o audio de tus plataformas favoritas con estilo!</b></p>
  <p><img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"> <img src="https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"> <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white" alt="Jetpack Compose"> <img src="https://img.shields.io/badge/yt--dlp-FF0000?style=for-the-badge&logo=youtube&logoColor=white" alt="yt-dlp"> <img src="https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge" alt="GPLv3 License"></p>
</div>

---

## ✨ Características

- 🎨 **Diseño Moderno:** Interfaz 100% nativa construida con Jetpack Compose y Material 3.
- 🌗 **Temas Personalizados:** Elige entre el tema Oscuro por defecto, un tema Claro, ¡o el exclusivo y vibrante tema **Poke** rosa/lila!
- ⚡ **Motor Potenciado:** Descargas veloces y estables impulsadas por la librería líder `yt-dlp`.
- 🎶 **Metadatos y Carátulas:** Descarga canciones con sus metadatos internos y su miniatura cuadrada original incrustada perfectamente como carátula de disco.
- 🔗 **Integración con Android:** Olvídate de copiar y pegar. Usa el botón "Compartir" en la app oficial de YouTube o TikTok y envíalo directamente a Patatatube.
- 📱 **Descargas en Segundo Plano:** Las descargas continúan sin inmutarse aunque minimices la aplicación, gracias a su sistema de servicios en primer plano.
- 🔄 **Comprobador de Actualizaciones:** Mantén pulsada la versión abajo en el centro para buscar e instalar nuevas actualizaciones directamente desde la app.
- 🖥️ **Terminal Integrada:** Botón flotante para abrir una consola estilo "hacker" y ver el progreso y los registros en tiempo real.
- 🚀 **Auto-Actualización del Motor:** Al instalar la app, el motor de descargas busca automáticamente la última versión compatible para estar siempre al día con los cambios de YouTube.
## 📸 Capturas de Pantalla

<p align="center">
  <img src="screenshots/screenshot_dark.jpg" width="30%">
  <img src="screenshots/screenshot_light.jpg" width="30%">
  <img src="screenshots/screenshot_pink.jpg" width="30%">
</p>

## 🛠️ Tecnologías Utilizadas

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose (Material Design 3)
- **Asincronía:** Coroutines & StateFlow
- **Motor de Descarga:** [youtubedl-android](https://github.com/yausername/youtubedl-android) (Wrapper de `yt-dlp` para Android)
- **Arquitectura:** Patrón Singleton para el Estado + Foreground Services.

## 🚀 Instalación y Compilación

### Requisitos Previos
- Android Studio (Versión Iguana o superior recomendada)
- JDK 17 o superior.

### Pasos

1. Clona este repositorio:
   ```bash
   git clone https://github.com/tu-usuario/patatatube-android.git
   ```
2. Abre el proyecto en Android Studio.
3. Conecta tu dispositivo Android (físico o emulador).
4. Dale al botón de **Run** (`Shift + F10`) o compila desde la terminal:
   ```bash
   ./gradlew assembleDebug
   ```

## 🤫 Easter Eggs

¡Patatatube tiene secretos escondidos!
- **Actualizar yt-dlp:** Mantén pulsado el botón de la Terminal (`>_`) durante medio segundo para forzar una actualización del motor de descargas en segundo plano.
- **Créditos:** Mantén pulsado el botón de Temas (🎨) para ver quiénes son las mentes creativas detrás del diseño y la programación.

## 📄 Licencia

Este proyecto se distribuye bajo la licencia **GNU GPLv3**. Esto significa que Patatatube es y siempre será libre y de código abierto. Eres libre de usarlo, estudiarlo, compartirlo y modificarlo, siempre y cuando cualquier trabajo derivado también sea de código abierto bajo esta misma licencia. ¡Larga vida al software libre!

---
<div align="center">
  <i>Diseñado por <b>pokeinalover</b> | Programado con ❤️ por <b>ContratopDev</b></i>
</div>
