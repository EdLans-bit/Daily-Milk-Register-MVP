package com.emiliano.lechapp

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.emiliano.lechapp.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val lecheViewModel: LecheViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        LecheViewModel.Factory(
            database.usuarioDao(),
            database.registrosRelacionalesDao(),
            GeminiService(),
        )
    }

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.get(0)?.let { spokenText ->
                lecheViewModel.procesarEntradaVoz(this, spokenText)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()

        // Cargar fragmento inicial con seguridad de renderizado
        if (savedInstanceState == null) {
            binding.root.post {
                if (!isFinishing && !isDestroyed) {
                    selectNavigationItem(binding.btnNavRegistro)
                }
            }
        }

        binding.btnSimularPremium.setOnClickListener {
            lecheViewModel.activarModoDemoPremium()
            Toast.makeText(this, "Funciones Premium Desbloqueadas", Toast.LENGTH_SHORT).show()
            binding.btnSimularPremium.visibility = View.GONE
        }

        binding.configTopBtn.setOnClickListener {
            replaceFragment(ConfiguracionFragment())
        }

        binding.navHostFragment.visibility = View.VISIBLE
    }

    private fun setupNavigation() {
        binding.btnNavRegistro.setOnClickListener { selectNavigationItem(it) }
        binding.btnNavDashboard.setOnClickListener { selectNavigationItem(it) }
        binding.btnNavEstadisticas.setOnClickListener { selectNavigationItem(it) }
        binding.btnNavGanado.setOnClickListener { selectNavigationItem(it) }
        
        binding.configTopBtn.setOnClickListener {
            replaceFragment(PerfilFragment())
        }
    }

    private fun selectNavigationItem(view: View) {
        if (view !is android.widget.LinearLayout) return
        
        // Reset all colors
        resetNavItem(binding.btnNavRegistro)
        resetNavItem(binding.btnNavDashboard)
        resetNavItem(binding.btnNavEstadisticas)
        resetNavItem(binding.btnNavGanado)

        // Highlight selected
        val icon = view.getChildAt(0) as? ImageView ?: return
        val text = view.getChildAt(1) as? TextView ?: return
        icon.setColorFilter(ContextCompat.getColor(this, R.color.primary_green))
        text.setTextColor(ContextCompat.getColor(this, R.color.primary_green))
        text.setTypeface(null, android.graphics.Typeface.BOLD)

        // Switch Fragment
        when (view.id) {
            R.id.btn_nav_registro -> replaceFragment(RegistroFragment())
            R.id.btn_nav_dashboard -> replaceFragment(SaludDashboardFragment())
            R.id.btn_nav_estadisticas -> replaceFragment(EstadisticasFragment())
            R.id.btn_nav_ganado -> replaceFragment(GestionAnimalesFragment())
        }
    }

    private fun resetNavItem(view: View) {
        if (view !is android.widget.LinearLayout) return
        val icon = view.getChildAt(0) as? ImageView ?: return
        val text = view.getChildAt(1) as? TextView ?: return
        icon.setColorFilter(ContextCompat.getColor(this, R.color.text_muted))
        text.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
        text.setTypeface(null, android.graphics.Typeface.NORMAL)
    }

    private fun replaceFragment(fragment: Fragment) {
        if (isFinishing || isDestroyed) return
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            lecheViewModel.activarMicrofonoDesdeHardware()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun iniciarReconocimientoVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta los datos (ej: 50 litros a Juan)")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (_: Exception) {
            // Manejar error si no hay motor de búsqueda por voz
        }
    }
}