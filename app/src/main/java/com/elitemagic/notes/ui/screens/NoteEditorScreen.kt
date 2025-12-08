package com.elitemagic.notes.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.elitemagic.notes.model.DrawingPath
import com.elitemagic.notes.model.DrawingPoint
import com.elitemagic.notes.model.Note
import com.elitemagic.notes.voice.VoiceRecognitionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: Note?,
    voiceManager: VoiceRecognitionManager,
    onSave: (Note) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var isDrawingMode by remember { mutableStateOf(false) }
    var drawingPaths by remember { mutableStateOf<List<DrawingPath>>(emptyList()) }
    var currentPath by remember { mutableStateOf<MutableList<DrawingPoint>>(mutableListOf()) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    val isListening by voiceManager.isListening.collectAsState()
    val recognizedText by voiceManager.recognizedText.collectAsState()
    val error by voiceManager.error.collectAsState()

    val hasAudioPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceDialog = true
        }
    }

    // Handle recognized text
    LaunchedEffect(recognizedText) {
        recognizedText?.let { text ->
            // Check if it's a playing card
            if (isPlayingCard(text)) {
                // Draw the card
                val cardPath = createCardDrawing(text)
                drawingPaths = drawingPaths + cardPath
            } else {
                // Add text to content
                content = if (content.isEmpty()) {
                    text
                } else {
                    "$content $text"
                }
            }
            voiceManager.clearRecognizedText()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (hasAudioPermission) {
                            showVoiceDialog = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Comando mágico",
                            tint = if (isListening) Color.Red else Color.Gray
                        )
                    }

                    IconButton(onClick = { isDrawingMode = !isDrawingMode }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Dibujar",
                            tint = if (isDrawingMode) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    IconButton(onClick = {
                        val noteToSave = note?.copy(
                            title = title,
                            content = content
                        ) ?: Note(
                            title = title,
                            content = content
                        )
                        onSave(noteToSave)
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Title field
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        "Título",
                        color = Color.LightGray,
                        fontSize = 20.sp
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Text(
                text = "8 de diciembre 9:00  |  0 caracteres",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Divider(color = Color.LightGray, thickness = 0.5.dp)

            // Content area
            if (isDrawingMode) {
                DrawingCanvas(
                    paths = drawingPaths,
                    currentPath = currentPath,
                    onPathUpdate = { newPoint ->
                        currentPath.add(newPoint)
                    },
                    onPathEnd = {
                        if (currentPath.isNotEmpty()) {
                            drawingPaths = drawingPaths + DrawingPath(
                                points = currentPath.toList(),
                                color = Color.Black.value.toLong(),
                                strokeWidth = 5f
                            )
                            currentPath.clear()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // Drawing toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        if (drawingPaths.isNotEmpty()) {
                            drawingPaths = drawingPaths.dropLast(1)
                        }
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Deshacer")
                    }

                    IconButton(onClick = {
                        drawingPaths = emptyList()
                        currentPath.clear()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpiar")
                    }
                }
            } else {
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = {
                        Text(
                            "Empieza a escribir",
                            color = Color.LightGray
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        }

        // Voice command dialog
        if (showVoiceDialog) {
            VoiceCommandDialog(
                isListening = isListening,
                error = error,
                onStartListening = {
                    voiceManager.startContinuousListening()
                },
                onStopListening = {
                    voiceManager.stopListening()
                },
                onDismiss = {
                    voiceManager.stopListening()
                    showVoiceDialog = false
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.stopListening()
        }
    }
}

@Composable
fun DrawingCanvas(
    paths: List<DrawingPath>,
    currentPath: List<DrawingPoint>,
    onPathUpdate: (DrawingPoint) -> Unit,
    onPathEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onPathUpdate(DrawingPoint(offset.x, offset.y))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onPathUpdate(DrawingPoint(change.position.x, change.position.y))
                    },
                    onDragEnd = {
                        onPathEnd()
                    }
                )
            }
    ) {
        // Draw existing paths
        paths.forEach { drawingPath ->
            val path = Path()
            drawingPath.points.forEachIndexed { index, point ->
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                }
            }
            drawPath(
                path = path,
                color = Color(drawingPath.color.toULong()),
                style = Stroke(
                    width = drawingPath.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Draw current path
        if (currentPath.isNotEmpty()) {
            val path = Path()
            currentPath.forEachIndexed { index, point ->
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                }
            }
            drawPath(
                path = path,
                color = Color.Black,
                style = Stroke(
                    width = 5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun VoiceCommandDialog(
    isListening: Boolean,
    error: String?,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🎤 Comando Mágico")
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isListening)
                        "Escuchando... Di 'tu carta pensada es' seguido de la carta"
                    else
                        "Toca el botón para empezar a escuchar",
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (error != null) {
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        if (isListening) {
                            onStopListening()
                        } else {
                            onStartListening()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) Color.Red else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isListening) "Detener" else "Iniciar")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

fun isPlayingCard(text: String): Boolean {
    val lowerText = text.lowercase()
    val ranks = listOf("as", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez", "jota", "reina", "rey", "j", "q", "k", "a", "2", "3", "4", "5", "6", "7", "8", "9", "10")
    val suits = listOf("corazones", "diamantes", "tréboles", "picas", "copas", "oros", "espadas", "bastos")

    return ranks.any { lowerText.contains(it) } || suits.any { lowerText.contains(it) }
}

fun createCardDrawing(cardName: String): DrawingPath {
    // Create a simple card outline
    val points = mutableListOf<DrawingPoint>()

    // Draw a rectangle representing a card (centered at 200, 200)
    val centerX = 200f
    val centerY = 300f
    val width = 100f
    val height = 140f

    // Top line
    for (x in 0..100 step 5) {
        points.add(DrawingPoint(centerX - width / 2 + x, centerY - height / 2))
    }
    // Right line
    for (y in 0..140 step 5) {
        points.add(DrawingPoint(centerX + width / 2, centerY - height / 2 + y))
    }
    // Bottom line
    for (x in 100 downTo 0 step 5) {
        points.add(DrawingPoint(centerX - width / 2 + x, centerY + height / 2))
    }
    // Left line
    for (y in 140 downTo 0 step 5) {
        points.add(DrawingPoint(centerX - width / 2, centerY - height / 2 + y))
    }

    return DrawingPath(
        points = points,
        color = Color.Black.value.toLong(),
        strokeWidth = 4f
    )
}
