package com.elitemagic.notes.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elitemagic.notes.model.DrawingPath
import com.elitemagic.notes.model.DrawingPoint
import com.elitemagic.notes.model.Note
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    notes: List<Note>,
    onNoteClick: (Note) -> Unit,
    onNewNoteClick: () -> Unit,
    onDeleteNote: (Note) -> Unit = {},
    onNavigateToTasks: () -> Unit = {}
) {
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
                            contentDescription = "Notas"
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
    }
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
