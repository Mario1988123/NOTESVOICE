# EliteMagic Notes - App de Notas con Comando Mágico de Voz

Una aplicación de notas para Android con una función especial de reconocimiento de voz que permite transcribir palabras o cartas mágicas mediante comandos de voz.

## 🎯 Características Principales

- **📝 Toma de notas**: Interfaz similar a la app de notas nativa de Android
- **🎤 Comando Mágico de Voz**:
  - Di "tu carta pensada es" o "tu palabra pensada es" seguido de la palabra/carta
  - El sistema solo transcribe la palabra que viene después del comando mágico
  - Reconocimiento continuo hasta detectar el comando
- **✏️ Modo de dibujo**: Dibuja directamente en las notas
- **🎴 Dibujo automático de cartas**: Si mencionas una carta, se dibuja automáticamente
- **💾 Almacenamiento local**: Las notas se guardan en el dispositivo

## 🚀 Comandos Mágicos Soportados

El sistema reconoce los siguientes comandos mágicos en español:
- "tu carta pensada es"
- "tu palabra pensada es"
- "la carta pensada es"
- "la palabra pensada es"

## 🔧 Requisitos Técnicos

- Android SDK 24+ (Android 7.0 o superior)
- Kotlin 1.9.20
- Jetpack Compose
- Permiso de micrófono

## 📱 Cómo Usar

1. **Crear una nueva nota**: Toca el botón flotante "+" en la pantalla principal
2. **Activar el comando mágico**:
   - Toca el ícono del micrófono en el editor de notas
   - Concede permiso de micrófono si se solicita
   - Toca "Iniciar" en el diálogo
   - Di el comando mágico seguido de la palabra/carta
   - Ejemplo: "tu carta pensada es as de picas"
3. **Modo dibujo**: Toca el ícono del lápiz para activar el modo de dibujo
4. **Guardar**: Toca el botón de verificación ✓ para guardar la nota

## 🏗️ Estructura del Proyecto

```
app/
├── src/main/
│   ├── java/com/elitemagic/notes/
│   │   ├── MainActivity.kt
│   │   ├── model/
│   │   │   └── Note.kt
│   │   ├── data/
│   │   │   └── NotesRepository.kt
│   │   ├── viewmodel/
│   │   │   └── NotesViewModel.kt
│   │   ├── voice/
│   │   │   └── VoiceRecognitionManager.kt
│   │   └── ui/
│   │       ├── screens/
│   │       │   ├── NotesListScreen.kt
│   │       │   └── NoteEditorScreen.kt
│   │       └── theme/
│   │           ├── Theme.kt
│   │           └── Type.kt
│   └── res/
│       ├── values/
│       ├── drawable/
│       └── mipmap-anydpi-v26/
```

## 🛠️ Construcción del Proyecto

### Opción 1: Android Studio
1. Abre Android Studio
2. File → Open → Selecciona la carpeta del proyecto
3. Espera a que Gradle sincronice
4. Conecta un dispositivo o inicia un emulador
5. Run → Run 'app'

### Opción 2: Línea de comandos
```bash
# Compilar el proyecto
./gradlew build

# Instalar en dispositivo conectado
./gradlew installDebug

# Generar APK
./gradlew assembleDebug
# El APK estará en: app/build/outputs/apk/debug/app-debug.apk
```

## 🎨 Personalización

### Modificar comandos mágicos
Edita el archivo `VoiceRecognitionManager.kt` y modifica la lista `magicCommands`:

```kotlin
private val magicCommands = listOf(
    "tu carta pensada es",
    "tu palabra pensada es",
    // Añade tus propios comandos aquí
)
```

### Cambiar colores
Edita `app/src/main/res/values/colors.xml` para personalizar los colores de la app.

## 📝 Notas de Desarrollo

### Reconocimiento de Voz
- Utiliza la API nativa de Android `SpeechRecognizer`
- Modo de escucha continua que se reinicia automáticamente
- Procesamiento de resultados parciales para detección temprana del comando
- Configurado para español (es-ES)

### Almacenamiento
- Usa SharedPreferences con serialización JSON (Gson)
- Persistencia local sin necesidad de base de datos compleja
- Las notas se ordenan por fecha de actualización

### Interfaz
- Jetpack Compose con Material3
- Diseño adaptativo similar a Google Keep/Samsung Notes
- Grid de 2 columnas para las tarjetas de notas

## 🔐 Permisos

La app requiere el permiso `RECORD_AUDIO` para usar la función de comando mágico. El permiso se solicita en tiempo de ejecución cuando el usuario intenta usar la funcionalidad de voz.

## 🎭 Creado por EliteMagic

Aplicación diseñada para magos e ilusionistas que necesitan una forma discreta y mágica de registrar información durante sus actuaciones.

## 📄 Licencia

Este proyecto es de código abierto y está disponible para uso personal y educativo.

## 🐛 Solución de Problemas

### El reconocimiento de voz no funciona
- Verifica que hayas concedido el permiso de micrófono
- Asegúrate de tener conexión a internet (el reconocimiento de Google requiere conexión)
- Verifica que el idioma del sistema esté configurado en español

### La app no compila
- Asegúrate de tener instalado Android SDK 34
- Ejecuta `./gradlew clean` y luego `./gradlew build`
- Verifica que tienes Java 8+ instalado

### El ícono no aparece correctamente
- Los iconos están definidos como vectores XML para evitar problemas de compatibilidad
- Si hay problemas, puedes generar los PNG usando Android Asset Studio

## 🔄 Futuras Mejoras

- [ ] Soporte offline para reconocimiento de voz
- [ ] Sincronización en la nube
- [ ] Más comandos mágicos personalizables
- [ ] Dibujo mejorado de cartas con símbolos de palos
- [ ] Organización de notas por categorías
- [ ] Widget para la pantalla de inicio
- [ ] Modo oscuro

## 📧 Contacto

Para reportar bugs o sugerir mejoras, abre un issue en el repositorio.

---

**Desarrollado con ❤️ para la comunidad mágica**
