package com.thfcmaz01.linguaswift

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.thfcmaz01.linguaswift.data.LinguaSwiftDatabase
import com.thfcmaz01.linguaswift.ui.LinguaSwiftApp
import com.thfcmaz01.linguaswift.ui.LinguaSwiftTheme
import com.thfcmaz01.linguaswift.viewmodel.TranslatorViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var database: LinguaSwiftDatabase
    private lateinit var speech: TextToSpeech
    private var speechReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        database = Room.databaseBuilder(applicationContext, LinguaSwiftDatabase::class.java, "linguaswift-database").build()
        val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TranslatorViewModel(application, database.dao()) as T
            }
        }
        speech = TextToSpeech(this) { speechReady = it == TextToSpeech.SUCCESS }
        setContent {
            val vm: TranslatorViewModel = viewModel(factory = factory)
            LinguaSwiftTheme(dark = vm.state.dark) {
                LinguaSwiftApp(vm, ::speak)
            }
        }
    }

    private fun speak(text: String, code: String) {
        if (text.isBlank()) {
            Toast.makeText(this, "There is no text to read.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!speechReady) {
            Toast.makeText(this, "Text-to-Speech is still starting. Try again.", Toast.LENGTH_SHORT).show()
            return
        }
        val locale = when (code) {
            "autodetect" -> Locale.ENGLISH
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "pt" -> Locale("pt", "BR")
            else -> Locale.forLanguageTag(code)
        }
        if (speech.isLanguageAvailable(locale) < TextToSpeech.LANG_AVAILABLE) {
            Toast.makeText(this, "No installed speech voice for this language. Check Android Text-to-Speech settings.", Toast.LENGTH_LONG).show()
            return
        }
        speech.language = locale
        speech.setSpeechRate(0.95f)
        speech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "linguaswift-" + System.currentTimeMillis())
    }

    override fun onDestroy() {
        if (::speech.isInitialized) { speech.stop(); speech.shutdown() }
        if (::database.isInitialized) database.close()
        super.onDestroy()
    }
}
