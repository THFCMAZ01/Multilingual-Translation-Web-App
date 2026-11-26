package com.thfcmaz01.linguaswift.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thfcmaz01.linguaswift.data.MyMemoryApi
import com.thfcmaz01.linguaswift.data.TranslationDao
import com.thfcmaz01.linguaswift.data.TranslationEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TranslatorState(
    val input: String = "",
    val output: String = "",
    val source: String = "en",
    val target: String = "fr",
    val loading: Boolean = false,
    val error: String? = null,
    val dark: Boolean = false,
    val lastId: Long? = null
)

class TranslatorViewModel(application: Application, private val dao: TranslationDao) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("linguaswift", 0)
    var state by mutableStateOf(TranslatorState(dark = prefs.getBoolean("dark", false)))
        private set
    val history = dao.history().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favourites = dao.favourites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun input(value: String) { state = state.copy(input = value.take(500), error = null) }
    fun source(value: String) { state = state.copy(source = value, error = null) }
    fun target(value: String) { state = state.copy(target = value, error = null) }
    fun theme() {
        val next = !state.dark
        prefs.edit().putBoolean("dark", next).apply()
        state = state.copy(dark = next)
    }
    fun swap() {
        if (state.source == "autodetect") {
            state = state.copy(error = "Choose a source language before swapping.")
        } else {
            state = state.copy(
                input = state.output.ifBlank { state.input }, output = "",
                source = state.target, target = state.source, lastId = null, error = null
            )
        }
    }
    fun translate() {
        if (state.loading) return
        val text = state.input.trim()
        if (text.isBlank()) { state = state.copy(error = "Enter some text first."); return }
        if (state.source != "autodetect" && state.source == state.target) {
            state = state.copy(error = "Choose two different languages.")
            return
        }
        val from = state.source
        val to = state.target
        state = state.copy(loading = true, output = "", error = null, lastId = null)
        viewModelScope.launch {
            try {
                val result = MyMemoryApi.translate(text, from, to)
                val id = dao.insert(TranslationEntity(sourceText = text, translatedText = result, sourceCode = from, targetCode = to))
                state = state.copy(output = result, loading = false, lastId = id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                state = state.copy(loading = false, error = error.message ?: "Translation failed. Check your connection.")
            }
        }
    }
    fun saveLast() {
        val id = state.lastId ?: return
        viewModelScope.launch {
            val item = dao.find(id) ?: return@launch
            dao.favourite(id, !item.isFavourite)
        }
    }
    fun toggle(item: TranslationEntity) { viewModelScope.launch { dao.favourite(item.id, !item.isFavourite) } }
    fun delete(item: TranslationEntity) { viewModelScope.launch { dao.delete(item.id) } }
}
