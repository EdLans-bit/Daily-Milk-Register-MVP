package com.emiliano.lechapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val VerdeOscuro = Color(0xFF1E5631)
val GrisFondo = Color(0xFFF8F9FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaGanado(viewModel: LecheViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "La Italiana",
                        color = VerdeOscuro,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO: Configuración */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = VerdeOscuro
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(GrisFondo)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Título de la vista
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pets, // Usamos Pets como placeholder de vaca
                    contentDescription = "Icono Vaca",
                    tint = VerdeOscuro,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Ganado",
                    color = VerdeOscuro,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botón Agregar
            Button(
                onClick = { /* TODO: Agregar Vaca */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeOscuro),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(14.dp)
            ) {
                Text(
                    "+ Agregar Nueva Vaca / Lote",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Inventario de Rendimiento (Hoy)",
                color = Color(0xFF1A1A1A),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Item Ganado 1: Vaca Mariposa
            ItemGanado(
                index = 1,
                nombre = "Vaca Mariposa",
                onEditar = { /* TODO */ },
                onEliminar = { /* TODO */ }
            )
        }
    }
}

@Composable
fun ItemGanado(index: Int, nombre: String, onEditar: () -> Unit, onEliminar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(0.dp) // Layout original no parece tener bordes redondeados pronunciados aquí
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge circular
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.Transparent, shape = CircleShape)
                    .padding(1.dp), // Simula el borde si es necesario, o usa border()
                contentAlignment = Alignment.Center
            ) {
                // El XML usa un drawable bg_circle_badge. Aquí lo simplificamos.
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(2.dp, VerdeOscuro),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = index.toString(),
                            color = VerdeOscuro,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = nombre,
                modifier = Modifier.weight(1f),
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.Bold
            )

            Row {
                TextButton(onClick = onEditar) {
                    Text("Editar", color = Color(0xFF1A1A1A), fontSize = 12.sp)
                }
                TextButton(onClick = onEliminar) {
                    Text("Eliminar", color = Color(0xFFA51D24), fontSize = 12.sp)
                }
            }
        }
    }
}
