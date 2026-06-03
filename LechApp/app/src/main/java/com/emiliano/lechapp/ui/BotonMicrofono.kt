package com.emiliano.lechapp.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.SharedFlow
import java.util.Locale

@Composable
fun BotonMicrofono(
    onTextoEscuchado: (String) -> Unit,
    modifier: Modifier = Modifier,
    externalTrigger: SharedFlow<Unit>? = null
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    
    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Ocupado..."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No te entendí"
                    else -> "Error: $error"
                }
                if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onTextoEscuchado(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose { speechRecognizer.destroy() }
    }

    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) speechRecognizer.startListening(intent)
        else Toast.makeText(context, "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    val handleAction = {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Escuchar el disparador externo (Botones de Volumen)
    LaunchedEffect(externalTrigger) {
        externalTrigger?.collect {
            handleAction()
        }
    }

    FloatingActionButton(
        onClick = { handleAction() },
        modifier = modifier,
        containerColor = if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
            contentDescription = "Micrófono"
        )
    }
}
