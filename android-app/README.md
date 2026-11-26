# LinguaSwift Android

Native Android companion for the LinguaSwift multilingual translator.

- Kotlin and Jetpack Compose
- Same 13 languages as the web app, including Swahili
- MyMemory Translation API
- Android native Text-to-Speech
- Language swapping, copy and share
- Room database for local history and favourites
- Light and dark themes

Minimum SDK is API 24 (Android 7.0), so Android 11 (API 30) is supported. Translation requires internet access. Previously translated items are stored locally and remain available offline. Speech availability depends on voices installed on the device.

Build from this directory with ./gradlew :app:assembleDebug. The APK is generated at app/build/outputs/apk/debug/app-debug.apk.

GitHub Actions builds the debug APK and publishes the LinguaSwift-debug-APK artifact. Open Actions, select a successful Build LinguaSwift Android APK run, and download the artifact.
