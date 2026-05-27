package com.emiliano.lechapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emiliano.lechapp.ui.theme.LechAppTheme
import com.emiliano.lechapp.ui.BotonMicrofono
import android.content.Intent

class MainActivity : ComponentActivity() {
    private lateinit var lecheViewModel: LecheViewModel

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || 
            keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (::lecheViewModel.isInitialized) {
                lecheViewModel.activarMicrofonoDesdeHardware()
                return true // Consumimos el evento para que no cambie el volumen
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val usuarioDao = database.usuarioDao()
        val registroViewModel = RegistroViewModel(usuarioDao)   // ← solo aquí, una vez
        val geminiService = GeminiService()
        val lecheViewModel = LecheViewModel(usuarioDao, geminiService)
        this.lecheViewModel = lecheViewModel // Guardamos referencia para los botones físicos

        setContent {
            LechAppTheme {
                var usuarioRegistrado by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(Unit) {
                    val perfil = usuarioDao.obtenerPerfil()
                    usuarioRegistrado = (perfil != null)
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (usuarioRegistrado) {
                        null -> {
                            // Cargando: muestra spinner mientras consulta la BD
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        false -> {
                            // Primera vez: mostrar pantalla de registro
                            PantallaRegistro { nombre, animales ->
                                registroViewModel.guardarGanadero(nombre, animales)
                                usuarioRegistrado = true
                            }
                        }
                        true -> {
                            // Ya registrado: mostrar navegación por pestañas
                            NavegacionPrincipal(lecheViewModel)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun PantallaRegistro(onRegistroExitoso: (String, String) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var animales by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Registro de Ganadero", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre Completo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = animales,
            onValueChange = { animales = it },
            label = { Text("Número de animales") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (nombre.isNotBlank() && animales.isNotBlank()) {
                    onRegistroExitoso(nombre, animales)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = nombre.isNotBlank() && animales.isNotBlank()
        ) {
            Text("Comenzar")
        }
    }
}

