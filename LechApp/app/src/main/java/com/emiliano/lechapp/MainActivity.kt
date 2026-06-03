package com.emiliano.lechapp

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var lecheViewModel: LecheViewModel
    
    private lateinit var secRegistro: LinearLayout
    private lateinit var secDashboard: LinearLayout
    private lateinit var secEstadisticas: LinearLayout
    private lateinit var secGanado: LinearLayout
    private lateinit var secConfiguracion: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar ViewModels y BD
        val database = AppDatabase.getDatabase(this)
        val usuarioDao = database.usuarioDao()
        val geminiService = GeminiService()
        lecheViewModel = LecheViewModel(usuarioDao, geminiService)

        // Referencias a las secciones
        secRegistro = findViewById(R.id.secRegistro)
        secDashboard = findViewById(R.id.secDashboard)
        secEstadisticas = findViewById(R.id.secEstadisticas)
        secGanado = findViewById(R.id.secGanado)
        secConfiguracion = findViewById(R.id.secConfiguracion)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            hideAllSections()
            when (item.itemId) {
                R.id.menu_registro -> secRegistro.visibility = View.VISIBLE
                R.id.menu_dashboard -> secDashboard.visibility = View.VISIBLE
                R.id.menu_estadisticas -> secEstadisticas.visibility = View.VISIBLE
                R.id.menu_ganado -> secGanado.visibility = View.VISIBLE
                R.id.menu_config -> secConfiguracion.visibility = View.VISIBLE
            }
            true
        }

        // Lógica de Registro
        val etLitros = findViewById<android.widget.EditText>(R.id.etLitros)
        val btnGuardar = findViewById<View>(R.id.btnGuardarRegistro)
        btnGuardar.setOnClickListener {
            val litros = etLitros.text.toString()
            lecheViewModel.guardarRegistroManual(this, litros, "", "")
            etLitros.text.clear()
        }

        // Lógica de Micrófono (Simplificada para demostración)
        val btnMic = findViewById<View>(R.id.btnMic)
        btnMic.setOnClickListener {
            // Aquí iría la lógica del SpeechRecognizer similar a BotonMicrofono.kt
            android.widget.Toast.makeText(this, "Escuchando...", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideAllSections() {
        secRegistro.visibility = View.GONE
        secDashboard.visibility = View.GONE
        secEstadisticas.visibility = View.GONE
        secGanado.visibility = View.GONE
        secConfiguracion.visibility = View.GONE
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || 
            keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            lecheViewModel.activarMicrofonoDesdeHardware()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
