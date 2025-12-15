# NOTEVOICE - App de Notas con Reconocimiento de Voz para Trucos de Magia
## Versión Final 1.0

**Desarrollado por**: EliteMagic
**Fecha**: Diciembre 2024
**APK**: NOTEVOICE.apk

---

## 📱 CARACTERÍSTICAS PRINCIPALES

### 1. **Modo Normal - Notas con Voz**
- Crea notas pulsando el botón `+`
- Activa el micrófono con el botón `T`
- Di el nombre de una carta de poker en español (ej: "as de picas", "reina de corazones")
- El micrófono se reinicia automáticamente cada 500ms hasta detectar una carta COMPLETA
- Solo para cuando detecta número + palo juntos
- Puedes dibujar activando el modo lápiz
- Las notas se ordenan por fecha de creación

### 2. **Modo Predicción - Truco de Magia** 🎩
El truco consiste en crear una nota con fecha/hora del PASADO para simular que ya sabías qué carta iba a elegir el espectador.

**Cómo activarlo:**
1. Mantén pulsado el icono "Notas" (barra inferior) durante 2+ segundos
2. Se abre el menú secreto de configuración
3. Activa el switch "Modo Predicción"
4. Selecciona una fecha/hora PASADA (ej: ayer a las 18:00)
5. Guarda

**Cómo usarlo:**
1. Pulsa el icono de carpeta 📁 (arriba a la derecha)
2. El icono se pone ROJO 🔴 indicando que está escuchando
3. El micrófono se activa automáticamente y se queda SIEMPRE activo
4. Di una carta (ej: "siete de diamantes")
5. La nota se crea AUTOMÁTICAMENTE con la fecha/hora pasada configurada
6. ¡La nota aparece en el pasado como si la hubieras escrito antes!

### 3. **Dibujar las 52 Cartas de Poker**
**Menú oculto en Tareas:**
1. Ve a la pestaña "Tareas" (barra inferior)
2. Mantén pulsado el icono "Tareas" durante 2+ segundos
3. Se abre una cuadrícula con las 52 cartas
4. Dibuja cada carta (as, 2-10, J, Q, K en cada palo: ♠️♥️♦️♣️)
5. Los dibujos se guardan automáticamente
6. Cuando dices una carta por voz, se carga TU dibujo

### 4. **Otras Características**
- **Borrar notas**: Desliza hacia la izquierda para eliminar
- **Preview de dibujos**: Las notas con dibujos muestran una vista previa
- **Pantalla activa**: La pantalla no se apaga mientras el micrófono está activo
- **Instrucciones**: Mantén pulsado el icono ⚙️ para ver las instrucciones completas

---

## 🎯 MENÚS OCULTOS (LONG-PRESS)

| Icono | Acción | Resultado |
|-------|--------|-----------|
| 📝 **Notas** (barra inferior) | Mantener 2+ segundos | Configuración del Modo Predicción |
| ⚙️ **Configuración** (arriba) | Mantener 2+ segundos | Diálogo de Instrucciones completas |
| 📋 **Tareas** (barra inferior) | Mantener 2+ segundos | Menú para dibujar las 52 cartas |

---

## 🗂️ ESTRUCTURA DEL CÓDIGO

```
app/src/main/java/com/elitemagic/notes/
├── MainActivity.kt                    # Actividad principal y navegación
├── data/
│   ├── CardsRepository.kt            # Gestión de dibujos de cartas
│   ├── NotesRepository.kt            # Gestión de notas (ordenadas por createdAt)
│   └── PredictionModeRepository.kt   # Gestión del modo predicción
├── model/
│   ├── Note.kt                       # Modelo de datos de nota
│   ├── DrawingPath.kt                # Modelo de trazos de dibujo
│   └── DrawingPoint.kt               # Puntos del dibujo
├── ui/
│   └── screens/
│       ├── NotesListScreen.kt        # Pantalla principal de notas
│       ├── NoteEditorScreen.kt       # Editor de notas con voz
│       └── TasksScreen.kt            # Menú de cartas para dibujar
├── viewmodel/
│   └── NotesViewModel.kt             # ViewModel con lógica de negocio
└── voice/
    └── VoiceRecognitionManager.kt    # Reconocimiento de voz en español
```

---

## 🎨 RECONOCIMIENTO DE VOZ

### Cartas Reconocidas (en español):

**Números:**
- As, Dos, Tres, Cuatro, Cinco, Seis, Siete, Ocho, Nueve, Diez, Jota/Jack, Reina/Dama/Queen, Rey/King

**Palos:**
- Picas/Espadas (♠️)
- Corazones (♥️)
- Diamantes (♦️)
- Tréboles (♣️)

**Ejemplos válidos:**
- "as de picas"
- "reina de corazones"
- "siete de diamantes"
- "jack de tréboles"

**IMPORTANTE**:
- Debe decir NÚMERO + PALO para que se detecte
- Si solo dice "as" o solo "picas" → NO se detecta (sigue escuchando)
- El micrófono se reinicia automáticamente hasta detectar carta completa

---

## 🔧 CONFIGURACIÓN TÉCNICA

### Build
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **Version**: 1.0 (versionCode: 1)

### APK Output
- **Nombre**: `NOTEVOICE.apk`
- **Ubicación**: `app/build/outputs/apk/debug/NOTEVOICE.apk`

### Dependencias Principales
- Jetpack Compose (Material3)
- Navigation Compose
- ViewModel & LiveData
- Gson (serialización JSON)
- Speech Recognition API

### Permisos
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

---

## 📦 CÓMO GENERAR EL APK

```bash
# Limpiar proyecto
./gradlew clean

# Generar APK debug
./gradlew assembleDebug

# APK generado en:
# app/build/outputs/apk/debug/NOTEVOICE.apk
```

---

## 🎭 FLUJO DEL TRUCO DE MAGIA

### Preparación (antes del espectador):
1. Dibuja todas las 52 cartas en el menú secreto de Tareas
2. Activa el modo predicción con una fecha/hora pasada
3. Ten la app lista en la pantalla de notas

### Ejecución (con el espectador):
1. Dile al espectador: "Ayer a las [hora configurada] escribí una predicción"
2. El espectador elige una carta al azar
3. Pulsa el icono de carpeta (se pone rojo)
4. Di el nombre de la carta elegida
5. La nota se crea automáticamente con fecha pasada
6. Muestra la lista de notas → ¡la nota aparece en el pasado!
7. Abre la nota → muestra tu dibujo de la carta

**Efecto**: Parece que predijiste la carta ANTES de que la eligieran 🎩✨

---

## 🐛 SOLUCIÓN DE PROBLEMAS

### El micrófono no se reinicia
- Verifica que tienes permiso de micrófono
- Comprueba que la función de auto-reinicio está activa (debería ser automático)

### No detecta las cartas
- Di claramente NÚMERO + PALO (ej: "as de picas")
- Habla en español
- Espera 1 segundo entre intentos

### El icono de carpeta no se pone rojo
- Asegúrate de tener el modo predicción activado
- Long-press en "Notas" → verifica que el switch está activado

### Las notas no se ordenan bien
- Se ordenan por `createdAt` (fecha de creación)
- En modo predicción, usa la fecha configurada
- En modo normal, usa la fecha actual

---

## 📝 CHANGELOG

### v1.0 (Versión Final)
- ✅ Reconocimiento de voz con auto-reinicio cada 500ms
- ✅ Modo predicción para trucos de magia
- ✅ Menú secreto para dibujar 52 cartas
- ✅ Preview de dibujos en lista de notas
- ✅ Ordenamiento por fecha de creación
- ✅ Selector de fecha/hora mejorado
- ✅ Diálogo de instrucciones completo
- ✅ Icono de app con lápiz diagonal
- ✅ APK con nombre personalizado: NOTEVOICE.apk
- ✅ Swipe para eliminar notas
- ✅ Pantalla activa durante grabación

---

## 👨‍💻 CRÉDITOS

**Desarrollado por**: EliteMagic
**Tecnología**: Kotlin + Jetpack Compose
**Plataforma**: Android (API 24+)

**Características únicas**:
- Sistema de predicción temporal para trucos de magia
- Reconocimiento de voz en español para cartas de poker
- Dibujos personalizables de las 52 cartas
- Menús secretos activados por long-press

---

## 📄 LICENCIA

Código privado - Todos los derechos reservados © EliteMagic 2024

---

## 🚀 PRÓXIMOS PASOS

La app está lista para distribución en Google Play Console.

**Para subir a Google Play:**
1. Genera un APK firmado (release)
2. Crea íconos en todas las resoluciones necesarias
3. Prepara capturas de pantalla
4. Escribe la descripción en Google Play Console
5. Configura edad, categoría, permisos
6. Sube el APK firmado

¡App lista para producción! 🎉
