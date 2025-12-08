package com.elitemagic.notes.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    var currentPath by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }

    // Drawing options
    var strokeWidth by remember { mutableStateOf(5f) }
    var selectedColor by remember { mutableStateOf(Color.Black) }

    val isListening by voiceManager.isListening.collectAsState()
    val recognizedText by voiceManager.recognizedText.collectAsState()
    val error by voiceManager.error.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            // Start listening automatically after permission is granted
            voiceManager.startContinuousListening()
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
                    // Microphone button with T icon
                    IconButton(onClick = {
                        if (hasAudioPermission) {
                            if (isListening) {
                                voiceManager.stopListening()
                            } else {
                                voiceManager.startContinuousListening()
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Activar/Desactivar micrófono",
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

            // Content area - Text and Drawing together
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Text field always visible
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
                    enabled = !isDrawingMode,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                )

                // Drawing canvas on top when drawing mode is active
                if (isDrawingMode) {
                    DrawingCanvas(
                        paths = drawingPaths,
                        currentPath = currentPath,
                        currentColor = selectedColor,
                        currentStrokeWidth = strokeWidth,
                        onPathUpdate = { newPoint ->
                            currentPath = currentPath.toMutableList().apply { add(newPoint) }
                        },
                        onPathEnd = {
                            if (currentPath.isNotEmpty()) {
                                drawingPaths = drawingPaths + DrawingPath(
                                    points = currentPath,
                                    color = selectedColor.value.toLong(),
                                    strokeWidth = strokeWidth
                                )
                                currentPath = emptyList()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Drawing toolbar with color and stroke options (only show when drawing mode is active)
            if (isDrawingMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(8.dp)
                ) {
                    // Color selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ColorButton(Color.Black, selectedColor == Color.Black) { selectedColor = Color.Black }
                        ColorButton(Color.Blue, selectedColor == Color.Blue) { selectedColor = Color.Blue }
                        ColorButton(Color.Red, selectedColor == Color.Red) { selectedColor = Color.Red }
                        ColorButton(Color.Green, selectedColor == Color.Green) { selectedColor = Color.Green }
                        ColorButton(Color(0xFFFFB300), selectedColor == Color(0xFFFFB300)) { selectedColor = Color(0xFFFFB300) }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stroke width selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Grosor:", fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            StrokeButton(3f, strokeWidth == 3f) { strokeWidth = 3f }
                            StrokeButton(5f, strokeWidth == 5f) { strokeWidth = 5f }
                            StrokeButton(8f, strokeWidth == 8f) { strokeWidth = 8f }
                            StrokeButton(12f, strokeWidth == 12f) { strokeWidth = 12f }
                        }

                        Row {
                            IconButton(onClick = {
                                if (drawingPaths.isNotEmpty()) {
                                    drawingPaths = drawingPaths.dropLast(1)
                                }
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Deshacer", tint = Color.Gray)
                            }

                            IconButton(onClick = {
                                drawingPaths = emptyList()
                                currentPath = emptyList()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Limpiar", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.stopListening()
        }
    }
}

@Composable
fun ColorButton(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.padding(2.dp) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = if (color == Color.White) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun StrokeButton(width: Float, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFFE0E0E0) else Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width.dp)
                .clip(CircleShape)
                .background(Color.Black)
        )
    }
}

@Composable
fun DrawingCanvas(
    paths: List<DrawingPath>,
    currentPath: List<DrawingPoint>,
    currentColor: Color,
    currentStrokeWidth: Float,
    onPathUpdate: (DrawingPoint) -> Unit,
    onPathEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(Color.Transparent)
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

        // Draw current path with current color and stroke
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
                color = currentColor,
                style = Stroke(
                    width = currentStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

fun isPlayingCard(text: String): Boolean {
    val lowerText = text.lowercase()
    val ranks = listOf("as", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez", "jota", "reina", "rey", "j", "q", "k", "a", "2", "3", "4", "5", "6", "7", "8", "9", "10")
    val suits = listOf("corazones", "diamantes", "tréboles", "picas", "copas", "oros", "espadas", "bastos")

    return ranks.any { lowerText.contains(it) } || suits.any { lowerText.contains(it) }
}

fun createCardDrawing(cardName: String): DrawingPath {
    // Create a hand-drawn looking card with natural variations
    val points = mutableListOf<DrawingPoint>()
    val random = java.util.Random(cardName.hashCode().toLong())

    val centerX = 200f
    val centerY = 300f
    val width = 120f
    val height = 170f

    // Helper function to add natural variation to hand-drawn lines
    fun addNoise(value: Float): Float {
        return value + (random.nextFloat() - 0.5f) * 3
    }

    // Draw card outline with natural hand-drawn wobble
    // Top line (with slight curves)
    var x = centerX - width / 2
    while (x <= centerX + width / 2) {
        points.add(DrawingPoint(addNoise(x), addNoise(centerY - height / 2)))
        x += 3 + random.nextFloat() * 2
    }

    // Right line
    var y = centerY - height / 2
    while (y <= centerY + height / 2) {
        points.add(DrawingPoint(addNoise(centerX + width / 2), addNoise(y)))
        y += 3 + random.nextFloat() * 2
    }

    // Bottom line
    x = centerX + width / 2
    while (x >= centerX - width / 2) {
        points.add(DrawingPoint(addNoise(x), addNoise(centerY + height / 2)))
        x -= 3 + random.nextFloat() * 2
    }

    // Left line
    y = centerY + height / 2
    while (y >= centerY - height / 2) {
        points.add(DrawingPoint(addNoise(centerX - width / 2), addNoise(y)))
        y -= 3 + random.nextFloat() * 2
    }

    // Parse card name to draw rank and suit
    val lowerName = cardName.lowercase()

    // Draw rank symbol in corner
    val rankX = centerX - width / 2 + 15
    val rankY = centerY - height / 2 + 20

    // Draw a simple "3" if it's a three (example)
    if (lowerName.contains("tres") || lowerName.contains("3")) {
        // Draw number 3
        addNumber3(points, rankX, rankY, random)
    } else if (lowerName.contains("as") || lowerName.contains("a")) {
        // Draw letter A
        addLetterA(points, rankX, rankY, random)
    }

    // Draw suit symbol in center
    if (lowerName.contains("corazones") || lowerName.contains("corazón")) {
        addHeart(points, centerX, centerY, random)
    } else if (lowerName.contains("picas")) {
        addSpade(points, centerX, centerY, random)
    } else if (lowerName.contains("diamantes")) {
        addDiamond(points, centerX, centerY, random)
    } else if (lowerName.contains("tréboles") || lowerName.contains("trebol")) {
        addClub(points, centerX, centerY, random)
    }

    return DrawingPath(
        points = points,
        color = Color.Black.value.toLong(),
        strokeWidth = 3f
    )
}

fun addNumber3(points: MutableList<DrawingPoint>, x: Float, y: Float, random: java.util.Random) {
    val addNoise = { value: Float -> value + (random.nextFloat() - 0.5f) * 1.5f }
    // Top curve
    points.add(DrawingPoint(addNoise(x), addNoise(y)))
    points.add(DrawingPoint(addNoise(x + 5), addNoise(y)))
    points.add(DrawingPoint(addNoise(x + 8), addNoise(y + 3)))
    points.add(DrawingPoint(addNoise(x + 5), addNoise(y + 6)))
    // Middle
    points.add(DrawingPoint(addNoise(x + 8), addNoise(y + 9)))
    // Bottom curve
    points.add(DrawingPoint(addNoise(x + 5), addNoise(y + 12)))
    points.add(DrawingPoint(addNoise(x), addNoise(y + 12)))
}

fun addLetterA(points: MutableList<DrawingPoint>, x: Float, y: Float, random: java.util.Random) {
    val addNoise = { value: Float -> value + (random.nextFloat() - 0.5f) * 1.5f }
    // Left line
    points.add(DrawingPoint(addNoise(x), addNoise(y + 12)))
    points.add(DrawingPoint(addNoise(x + 4), addNoise(y)))
    // Right line
    points.add(DrawingPoint(addNoise(x + 8), addNoise(y + 12)))
    // Middle bar
    points.add(DrawingPoint(addNoise(x + 2), addNoise(y + 6)))
    points.add(DrawingPoint(addNoise(x + 6), addNoise(y + 6)))
}

fun addHeart(points: MutableList<DrawingPoint>, cx: Float, cy: Float, random: java.util.Random) {
    val addNoise = { value: Float -> value + (random.nextFloat() - 0.5f) * 2f }
    val size = 25f
    // Heart shape
    points.add(DrawingPoint(addNoise(cx), addNoise(cy - size / 4)))
    points.add(DrawingPoint(addNoise(cx - size / 2), addNoise(cy - size / 2)))
    points.add(DrawingPoint(addNoise(cx - size / 3), addNoise(cy - size)))
    points.add(DrawingPoint(addNoise(cx), addNoise(cy - size / 1.5f)))
    points.add(DrawingPoint(addNoise(cx + size / 3), addNoise(cy - size)))
    points.add(DrawingPoint(addNoise(cx + size / 2), addNoise(cy - size / 2)))
    points.add(DrawingPoint(addNoise(cx), addNoise(cy - size / 4)))
    points.add(DrawingPoint(addNoise(cx), addNoise(cy + size / 2)))
}

fun addSpade(points: MutableList<DrawingPoint>, cx: Float, cy: Float, random: java.util.Random) {
    val addNoise = { value: Float -> value + (random.nextFloat() - 0.5f) * 2f }
    val size = 25f
    // Spade shape (inverted heart with stem)
    points.add(DrawingPoint(addNoise(cx), addNoise(cy + size / 2)))
    points.add(DrawingPoint(addNoise(cx), addNoise(cy)))
    points.add(DrawingPoint(addNoise(cx - size / 2), addNoise(cy - size / 4)))
    points.add(DrawingPoint(addNoise(cx - size / 3), addNoise(cy - size / 2)))
    points.add(DrawingPoint(addNoise(cx), addNoise(cy - size)))
    points.add(DrawingPoint(addNoise(cx + size / 3), addNoise(cy - size / 2)))
    points.add(DrawingPoint(addNoise(cx + size / 2), addNoise(cy - size / 4)))
    points.add(DrawingPoint(addNoise(cx), addNoise(cy)))
}

fun addDiamond(points: MutableList<DrawingPoint>, cx: Float, cy: Float, random: java.util.Random) {
    val addNoise = { value: Float -> value + (random.nextFloat() - 0.5f) * 2f }
    val size = 30f
    // Diamond shape
    points.add(DrawingPoint(addNoise(cx), addNoise(cy - size / 2)))
    points.add(DrawingPoint(addNoise(cx + size / 2), addNoise(cy)))
    points.add(DrawingPoint(addNoise(cx), addNoise(cy + size / 2)))
    points.add(DrawingPoint(addNoise(cx - size / 2), addNoise(cy)))
    points.add(DrawingPoint(addNoise(cx), addNoise(cy - size / 2)))
}

fun addClub(points: MutableList<DrawingPoint>, cx: Float, cy: Float, random: java.util.Random) {
    val addNoise = { value: Float -> value + (random.nextFloat() - 0.5f) * 2f }
    val size = 20f
    // Three circles forming a club
    // Top circle
    for (angle in 0..360 step 30) {
        val rad = Math.toRadians(angle.toDouble())
        points.add(DrawingPoint(
            addNoise(cx + (size / 3 * Math.cos(rad)).toFloat()),
            addNoise(cy - size / 2 + (size / 3 * Math.sin(rad)).toFloat())
        ))
    }
    // Stem
    points.add(DrawingPoint(addNoise(cx), addNoise(cy)))
    points.add(DrawingPoint(addNoise(cx), addNoise(cy + size / 2)))
}
