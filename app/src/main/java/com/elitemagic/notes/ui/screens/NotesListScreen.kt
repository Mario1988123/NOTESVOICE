package com.elitemagic.notes.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
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
                    IconButton(onClick = { /* TODO: Open settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
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

    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }

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

