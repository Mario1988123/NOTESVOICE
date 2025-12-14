package com.elitemagic.notes.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elitemagic.notes.data.PredictionModeRepository
import com.elitemagic.notes.model.DrawingPath
import com.elitemagic.notes.model.DrawingPoint
import com.elitemagic.notes.model.Note
import com.elitemagic.notes.voice.VoiceRecognitionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesListScreen(
    notes: List<Note>,
    onNoteClick: (Note) -> Unit,
    onNewNoteClick: () -> Unit,
    onCreateNoteWithCard: (Note) -> Unit = {},
    onDeleteNote: (Note) -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    voiceManager: VoiceRecognitionManager? = null
) {
    val context = LocalContext.current
    val predictionRepo = remember { PredictionModeRepository(context) }
    val cardsRepository = remember { com.elitemagic.notes.data.CardsRepository(context) }
    val gson = remember { Gson() }

    var showConfigDialog by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }
    var isPredictionMode by remember { mutableStateOf(predictionRepo.isPredictionMode()) }
    var isListeningForCard by remember { mutableStateOf(false) }

    val isListening by voiceManager?.isListening?.collectAsState() ?: remember { mutableStateOf(false) }
    val recognizedText by voiceManager?.recognizedText?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
    val error by voiceManager?.error?.collectAsState() ?: remember { mutableStateOf<String?>(null) }

    // Actualizar el estado cuando cambia el modo
    LaunchedEffect(showConfigDialog) {
        if (!showConfigDialog) {
            isPredictionMode = predictionRepo.isPredictionMode()
        }
    }

    // DETECTAR Y PROCESAR comandos de cartas en modo predicción
    LaunchedEffect(recognizedText, isListeningForCard) {
        if (isListeningForCard && recognizedText != null) {
            android.util.Log.d("NotesListScreen", "Texto reconocido: $recognizedText")

            // Detectar si es una carta COMPLETA (número + palo)
            val detectedCards = detectCardsInText(recognizedText!!)

            if (detectedCards.isNotEmpty()) {
                // Es una carta COMPLETA - crear nota
                android.util.Log.d("NotesListScreen", "Carta detectada: ${detectedCards.first()}")

                val card = detectedCards.first()
                val savedPaths = cardsRepository.getCardDrawing(card)

                if (savedPaths != null && savedPaths.isNotEmpty()) {
                    // Crear nota con el dibujo y fecha personalizada
                    val drawingJson = gson.toJson(savedPaths)
                    val timestamp = predictionRepo.getTimestampForNewNote()

                    val newNote = com.elitemagic.notes.model.Note(
                        title = "",
                        content = "",
                        drawingData = drawingJson,
                        createdAt = timestamp,
                        updatedAt = timestamp
                    )

                    // Guardar la nota
                    onCreateNoteWithCard(newNote)

                    // Parar el micrófono y modo escucha
                    voiceManager?.stopListening()
                    isListeningForCard = false

                    android.util.Log.d("NotesListScreen", "Nota creada con carta: $card")
                }

                voiceManager?.clearRecognizedText()
            } else {
                // NO es carta completa - seguir escuchando
                android.util.Log.d("NotesListScreen", "No es carta completa, continuando escucha")
                voiceManager?.clearRecognizedText()
            }
        }
    }

    // Reiniciar micrófono automáticamente cuando hay error de timeout
    LaunchedEffect(error, isListeningForCard, isListening) {
        if (isListeningForCard && error != null && !isListening) {
            android.util.Log.d("NotesListScreen", "Error detectado: $error, reiniciando micrófono")
            kotlinx.coroutines.delay(500)
            voiceManager?.startListening()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notas",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal
                    )
                },
                actions = {
                    IconButton(onClick = {
                        if (isPredictionMode) {
                            // En modo predicción, activar/desactivar micrófono
                            if (isListeningForCard) {
                                voiceManager?.stopListening()
                                isListeningForCard = false
                            } else {
                                voiceManager?.startListening()
                                isListeningForCard = true
                            }
                        } else {
                            // En modo normal, abrir carpetas (no implementado)
                        }
                    }) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = "Carpetas",
                            tint = if (isPredictionMode && isListeningForCard) Color.Red else Color.Gray
                        )
                    }
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configuración",
                            modifier = Modifier.combinedClickable(
                                onClick = { },
                                onLongClick = { showInstructionsDialog = true }
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewNoteClick,
                containerColor = MaterialTheme.colorScheme.secondary,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Nueva nota",
                    tint = Color.White
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "Notas",
                            modifier = Modifier.combinedClickable(
                                onClick = { },
                                onLongClick = { showConfigDialog = true }
                            )
                        )
                    },
                    label = { Text("Notas") }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToTasks,
                    icon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Tareas"
                        )
                    },
                    label = { Text("Tareas") }
                )
            }
        }
    ) { paddingValues ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hay notas",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    val dismissState = rememberDismissState(
                        confirmValueChange = {
                            if (it == DismissValue.DismissedToStart || it == DismissValue.DismissedToEnd) {
                                onDeleteNote(note)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismiss(
                        state = dismissState,
                        background = {
                            val color = when (dismissState.dismissDirection) {
                                DismissDirection.StartToEnd, DismissDirection.EndToStart -> Color.Red
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = if (dismissState.dismissDirection == DismissDirection.StartToEnd) {
                                    Alignment.CenterStart
                                } else {
                                    Alignment.CenterEnd
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Borrar",
                                    tint = Color.White
                                )
                            }
                        },
                        dismissContent = {
                            NoteCard(
                                note = note,
                                onClick = { onNoteClick(note) }
                            )
                        }
                    )
                }
            }
        }

        // Diálogo de configuración del modo predicción
        if (showConfigDialog) {
            PredictionModeConfigDialog(
                predictionRepo = predictionRepo,
                onDismiss = { showConfigDialog = false }
            )
        }

        // Diálogo de instrucciones
        if (showInstructionsDialog) {
            InstructionsDialog(
                onDismiss = { showInstructionsDialog = false }
            )
        }
    }
}

@Composable
fun PredictionModeConfigDialog(
    predictionRepo: PredictionModeRepository,
    onDismiss: () -> Unit
) {
    var isPredictionEnabled by remember { mutableStateOf(predictionRepo.isPredictionMode()) }
    val currentTimestamp = predictionRepo.getCustomTimestamp()
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = currentTimestamp } }

    // Usar String para permitir edición fácil
    var dayText by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH).toString()) }
    var monthText by remember { mutableStateOf((calendar.get(Calendar.MONTH) + 1).toString()) }
    var yearText by remember { mutableStateOf(calendar.get(Calendar.YEAR).toString()) }
    var hourText by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')) }
    var minuteText by remember { mutableStateOf(calendar.get(Calendar.MINUTE).toString().padStart(2, '0')) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Modo Predicción",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Switch para activar/desactivar modo predicción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Activar modo predicción")
                    Switch(
                        checked = isPredictionEnabled,
                        onCheckedChange = { isPredictionEnabled = it }
                    )
                }

                if (isPredictionEnabled) {
                    Divider()

                    Text(
                        "Selecciona la fecha y hora:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Selector de fecha
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Día
                        OutlinedTextField(
                            value = dayText,
                            onValueChange = { dayText = it.filter { char -> char.isDigit() }.take(2) },
                            label = { Text("Día") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        // Mes
                        OutlinedTextField(
                            value = monthText,
                            onValueChange = { monthText = it.filter { char -> char.isDigit() }.take(2) },
                            label = { Text("Mes") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        // Año
                        OutlinedTextField(
                            value = yearText,
                            onValueChange = { yearText = it.filter { char -> char.isDigit() }.take(4) },
                            label = { Text("Año") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Selector de hora
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hora
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { hourText = it.filter { char -> char.isDigit() }.take(2) },
                            label = { Text("Hora") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        // Minutos
                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { minuteText = it.filter { char -> char.isDigit() }.take(2) },
                            label = { Text("Min") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Vista previa
                    val previewDay = dayText.toIntOrNull() ?: 1
                    val previewMonth = (monthText.toIntOrNull() ?: 1) - 1
                    val previewYear = yearText.toIntOrNull() ?: 2024
                    val previewHour = hourText.toIntOrNull() ?: 0
                    val previewMinute = minuteText.toIntOrNull() ?: 0

                    val previewCalendar = Calendar.getInstance().apply {
                        set(previewYear, previewMonth, previewDay, previewHour, previewMinute, 0)
                    }
                    val dateFormat = SimpleDateFormat("d MMMM yyyy HH:mm", Locale("es", "ES"))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "Vista previa:\n${dateFormat.format(previewCalendar.time)}",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    predictionRepo.setPredictionMode(isPredictionEnabled)

                    if (isPredictionEnabled) {
                        val day = dayText.toIntOrNull() ?: 1
                        val month = (monthText.toIntOrNull() ?: 1) - 1
                        val year = yearText.toIntOrNull() ?: 2024
                        val hour = hourText.toIntOrNull() ?: 0
                        val minute = minuteText.toIntOrNull() ?: 0

                        val timestamp = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        predictionRepo.setCustomTimestamp(timestamp)
                    }

                    onDismiss()
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit
) {
    // Formato: "14 diciembre 10:30"
    val dateFormat = SimpleDateFormat("d MMMM HH:mm", Locale("es", "ES"))
    val formattedDate = dateFormat.format(Date(note.updatedAt))

    // Parsear paths del dibujo si existen
    val drawingPaths = remember(note.drawingData) {
        note.drawingData?.let { json ->
            try {
                val gson = Gson()
                val type = object : TypeToken<List<DrawingPath>>() {}.type
                gson.fromJson<List<DrawingPath>>(json, type)
            } catch (e: Exception) {
                null
            }
        }
    }

    val hasDrawing = drawingPaths != null && drawingPaths.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp, max = 140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Mostrar preview del dibujo si existe
            if (hasDrawing && drawingPaths != null) {
                DrawingPreview(
                    paths = drawingPaths,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Overlay con info de la nota
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (hasDrawing) Color.White.copy(alpha = 0.85f) else Color.Transparent
                    )
                    .padding(10.dp)
            ) {
                if (note.title.isNotEmpty()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                if (!hasDrawing && note.content.isNotEmpty()) {
                    // Solo mostrar contenido de texto si NO hay dibujo
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        maxLines = if (note.title.isEmpty()) 6 else 4,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun DrawingPreview(
    paths: List<DrawingPath>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Calcular bounding box del dibujo
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        paths.forEach { drawingPath ->
            drawingPath.points.forEach { point ->
                if (point.x < minX) minX = point.x
                if (point.y < minY) minY = point.y
                if (point.x > maxX) maxX = point.x
                if (point.y > maxY) maxY = point.y
            }
        }

        // Calcular escala para que quepa en la vista
        val drawingWidth = maxX - minX
        val drawingHeight = maxY - minY
        val scale = minOf(
            size.width / drawingWidth,
            size.height / drawingHeight
        ) * 0.8f // 80% para dejar margen

        // Calcular offset para centrar
        val offsetX = (size.width - drawingWidth * scale) / 2 - minX * scale
        val offsetY = (size.height - drawingHeight * scale) / 2 - minY * scale

        // Dibujar paths escalados y centrados
        paths.forEach { drawingPath ->
            val path = Path()
            val scaledPoints = drawingPath.points.map { point ->
                Offset(
                    x = point.x * scale + offsetX,
                    y = point.y * scale + offsetY
                )
            }

            if (scaledPoints.isNotEmpty()) {
                path.moveTo(scaledPoints[0].x, scaledPoints[0].y)
                for (i in 1 until scaledPoints.size) {
                    path.lineTo(scaledPoints[i].x, scaledPoints[i].y)
                }

                drawPath(
                    path = path,
                    color = Color(drawingPath.color.toULong()),
                    style = Stroke(
                        width = drawingPath.strokeWidth * scale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
fun InstructionsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Instrucciones de Uso",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Modo Normal
                Text(
                    "🎴 Modo Normal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "1. Pulsa el botón + para crear una nota\n" +
                    "2. Pulsa la 'T' para activar el micrófono\n" +
                    "3. Di el nombre de una carta (ej: 'as de picas')\n" +
                    "4. El micrófono se reinicia automáticamente hasta detectar una carta completa\n" +
                    "5. Puedes dibujar pulsando el icono del lápiz",
                    fontSize = 14.sp
                )

                Divider()

                // Modo Predicción (Truco de Magia)
                Text(
                    "🎩 Modo Predicción (Truco de Magia)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "1. Mantén pulsado el icono 'Notas' (abajo) durante 2 segundos\n" +
                    "2. Activa el modo predicción y configura una fecha/hora pasada\n" +
                    "3. Guarda la configuración\n" +
                    "4. Pulsa el icono de carpeta (arriba) - se pondrá ROJO\n" +
                    "5. El micrófono se activa automáticamente\n" +
                    "6. Di una carta y se creará una nota con la fecha pasada\n" +
                    "7. ¡La nota aparecerá como si la hubieras escrito en el pasado!",
                    fontSize = 14.sp
                )

                Divider()

                // Menús Ocultos
                Text(
                    "🔒 Menús Ocultos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "• Mantén pulsado 'Notas' → Modo Predicción\n" +
                    "• Mantén pulsado ⚙️ → Este menú de ayuda\n" +
                    "• Mantén pulsado 'Tareas' → Dibujar las 52 cartas",
                    fontSize = 14.sp
                )

                Divider()

                // Firma
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Creado por EliteMagic",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}

