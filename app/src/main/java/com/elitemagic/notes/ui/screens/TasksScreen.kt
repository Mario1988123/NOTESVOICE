package com.elitemagic.notes.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elitemagic.notes.data.CardsRepository
import com.elitemagic.notes.model.DrawingPath
import com.elitemagic.notes.model.DrawingPoint
import kotlinx.coroutines.delay

data class CardInfo(
    val name: String,
    val suit: String,
    val fullName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onNavigateToNotes: () -> Unit
) {
    val context = LocalContext.current
    val cardsRepository = remember { CardsRepository(context) }

    var showCardMenu by remember { mutableStateOf(false) }
    var longPressTriggered by remember { mutableStateOf(false) }

    // Crear las 52 cartas de poker
    val cards = remember {
        val suits = listOf("♥ Corazones", "♦ Diamantes", "♣ Tréboles", "♠ Picas")
        val ranks = listOf("As", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
        val cardsList = mutableListOf<CardInfo>()

        for (suit in suits) {
            for (rank in ranks) {
                val fullName = "$rank de $suit"
                cardsList.add(CardInfo(rank, suit, fullName))
            }
        }
        cardsList
    }

    if (showCardMenu) {
        CardMenuScreen(
            cards = cards,
            cardsRepository = cardsRepository,
            onClose = { showCardMenu = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Tareas",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* TODO: Open folder view */ }) {
                            Icon(Icons.Default.Folder, contentDescription = "Carpetas")
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
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White
                ) {
                    NavigationBarItem(
                        selected = false,
                        onClick = onNavigateToNotes,
                        icon = {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = "Notas"
                            )
                        },
                        label = { Text("Notas") }
                    )
                    NavigationBarItem(
                        selected = true,
                        onClick = { },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                if (!longPressTriggered) {
                                    longPressTriggered = true
                                    showCardMenu = true
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No hay tareas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    // Reset del trigger cuando se cierra el menú
    LaunchedEffect(showCardMenu) {
        if (!showCardMenu) {
            delay(500)
            longPressTriggered = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardMenuScreen(
    cards: List<CardInfo>,
    cardsRepository: CardsRepository,
    onClose: () -> Unit
) {
    var selectedCard by remember { mutableStateOf<CardInfo?>(null) }

    if (selectedCard != null) {
        CardDrawingScreen(
            card = selectedCard!!,
            cardsRepository = cardsRepository,
            onBack = { selectedCard = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Baraja de Poker - 52 Cartas",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cards) { card ->
                    CardItem(
                        card = card,
                        hasDrawing = cardsRepository.hasCardDrawing(card.fullName),
                        onClick = { selectedCard = card }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardItem(
    card: CardInfo,
    hasDrawing: Boolean,
    onClick: () -> Unit
) {
    val suitColor = when {
        card.suit.contains("♥") || card.suit.contains("♦") -> Color(0xFFE53935)
        else -> Color.Black
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = card.name,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = card.suit,
                    fontSize = 14.sp,
                    color = suitColor,
                    fontWeight = FontWeight.Medium
                )

                // Indicador si ya tiene dibujos
                if (hasDrawing) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Dibujada",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDrawingScreen(
    card: CardInfo,
    cardsRepository: CardsRepository,
    onBack: () -> Unit
) {
    // COPIAR EXACTAMENTE EL PATRÓN DE NoteEditorScreen
    var drawingPaths by remember {
        mutableStateOf<List<DrawingPath>>(
            cardsRepository.getCardDrawing(card.fullName)?.also {
                android.util.Log.d("CardDrawing", "Cargados ${it.size} paths para ${card.fullName}")
            } ?: emptyList<DrawingPath>().also {
                android.util.Log.d("CardDrawing", "No hay paths guardados para ${card.fullName}")
            }
        )
    }
    var currentPath by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }
    var strokeWidth by remember { mutableStateOf(5f) }
    var selectedColor by remember { mutableStateOf(Color.Black) }

    // Colores disponibles
    val availableColors = listOf(
        Color.Black,
        Color.Red,
        Color.Blue,
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFF795548)  // Brown
    )

    // Función para guardar
    val saveDrawing = {
        android.util.Log.d("CardDrawing", "Guardando ${drawingPaths.size} paths para ${card.fullName}")
        cardsRepository.saveCardDrawing(card.fullName, drawingPaths)
        android.util.Log.d("CardDrawing", "Guardado completado")
    }

    // Guardar automáticamente cuando cambien los paths
    LaunchedEffect(drawingPaths) {
        android.util.Log.d("CardDrawing", "LaunchedEffect triggered: ${drawingPaths.size} paths")
        if (drawingPaths.isNotEmpty()) {
            saveDrawing()
        }
    }

    // Guardar cuando se destruye el composable
    DisposableEffect(Unit) {
        onDispose {
            saveDrawing()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            card.fullName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Dibuja la carta con tu dedo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        saveDrawing()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón para borrar todo
                    IconButton(onClick = {
                        drawingPaths = emptyList()
                        cardsRepository.deleteCardDrawing(card.fullName)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar todo")
                    }
                    // Botón para deshacer último trazo
                    IconButton(
                        onClick = {
                            if (drawingPaths.isNotEmpty()) {
                                drawingPaths = drawingPaths.dropLast(1)
                            }
                        },
                        enabled = drawingPaths.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Deshacer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Selector de color
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Color:", fontWeight = FontWeight.Medium)
                        availableColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(if (color == selectedColor) 42.dp else 36.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = if (color == selectedColor) 3.dp else 1.dp,
                                        color = if (color == selectedColor) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selector de grosor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grosor:", fontWeight = FontWeight.Medium)

                        Button(
                            onClick = { strokeWidth = 3f },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (strokeWidth == 3f) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                        ) {
                            Text("Fino")
                        }

                        Button(
                            onClick = { strokeWidth = 5f },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (strokeWidth == 5f) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                        ) {
                            Text("Normal")
                        }

                        Button(
                            onClick = { strokeWidth = 8f },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (strokeWidth == 8f) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                        ) {
                            Text("Grueso")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // MISMO CÓDIGO QUE DrawingCanvas en NoteEditorScreen
            DrawingCanvasForCards(
                paths = drawingPaths,
                currentPath = currentPath,
                currentColor = selectedColor,
                currentStrokeWidth = strokeWidth,
                onPathUpdate = { newPoint ->
                    currentPath = currentPath.toMutableList().apply { add(newPoint) }
                },
                onPathEnd = {
                    android.util.Log.d("CardDrawing", "onPathEnd called, currentPath size: ${currentPath.size}")
                    if (currentPath.isNotEmpty()) {
                        val newPath = DrawingPath(
                            points = currentPath,
                            color = selectedColor.value.toLong(),
                            strokeWidth = strokeWidth
                        )
                        android.util.Log.d("CardDrawing", "Adding path with ${newPath.points.size} points")
                        drawingPaths = drawingPaths + newPath
                        android.util.Log.d("CardDrawing", "Total paths now: ${drawingPaths.size}")
                        currentPath = emptyList()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color.LightGray)
            )
        }
    }
}

// Canvas de dibujo - MISMO CÓDIGO QUE EN NoteEditorScreen
@Composable
fun DrawingCanvasForCards(
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
