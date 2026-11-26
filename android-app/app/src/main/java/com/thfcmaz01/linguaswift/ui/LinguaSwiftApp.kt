package com.thfcmaz01.linguaswift.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thfcmaz01.linguaswift.data.TranslationEntity
import com.thfcmaz01.linguaswift.viewmodel.TranslatorViewModel
import java.text.DateFormat
import java.util.Date

private data class Language(val code: String, val label: String)
private val languages = listOf(
    Language("autodetect", "Detect language"), Language("en", "English"), Language("fr", "French"),
    Language("es", "Spanish"), Language("de", "German"), Language("pt", "Portuguese"),
    Language("it", "Italian"), Language("zh", "Chinese"), Language("ja", "Japanese"),
    Language("ar", "Arabic"), Language("ru", "Russian"), Language("ko", "Korean"),
    Language("hi", "Hindi"), Language("sw", "Swahili")
)
private fun languageName(code: String) = languages.firstOrNull { it.code == code }?.label ?: code
private enum class Tab(val label: String, val symbol: String) {
    TRANSLATE("Translate", "⇄"), SAVED("Saved", "★"), HISTORY("History", "◷")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinguaSwiftApp(vm: TranslatorViewModel, onSpeak: (String, String) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.TRANSLATE.name) }
    val state = vm.state
    val history by vm.history.collectAsState()
    val favourites by vm.favourites.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Row { Text("Lingua"); Text("Swift", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } },
                actions = { TextButton(onClick = vm::theme) { Text(if (state.dark) "Light" else "Dark") } }
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.values().forEach { item ->
                    NavigationBarItem(selected = tab == item.name, onClick = { tab = item.name },
                        icon = { Text(item.symbol, fontSize = 18.sp) }, label = { Text(item.label) })
                }
            }
        }
    ) { padding ->
        when (Tab.valueOf(tab)) {
            Tab.TRANSLATE -> TranslatorScreen(Modifier.padding(padding), vm, onSpeak, favourites.any { it.id == state.lastId })
            Tab.SAVED -> RecordsScreen(Modifier.padding(padding), "Saved translations", favourites, "Your saved translations will appear here.", vm, onSpeak)
            Tab.HISTORY -> RecordsScreen(Modifier.padding(padding), "Recent translations", history, "Your recent translations will appear here.", vm, onSpeak)
        }
    }
}

@Composable
private fun TranslatorScreen(modifier: Modifier, vm: TranslatorViewModel, onSpeak: (String, String) -> Unit, isSaved: Boolean) {
    val state = vm.state
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Translate naturally", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Translate between 13 languages.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("From")
                        LanguagePicker(state.source, languages, vm::source)
                    }
                    TextButton(onClick = vm::swap) { Text("⇄", fontSize = 22.sp) }
                    Column(Modifier.weight(1f)) {
                        Text("To")
                        LanguagePicker(state.target, languages.filterNot { it.code == "autodetect" }, vm::target)
                    }
                }
                OutlinedTextField(
                    value = state.input, onValueChange = vm::input, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Text to translate") }, placeholder = { Text("Type or paste text…") },
                    minLines = 4, maxLines = 6,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    supportingText = { Text(state.input.length.toString() + "/500") }
                )
                Row {
                    TextButton(onClick = { onSpeak(state.input, state.source) }, enabled = state.input.isNotBlank()) { Text("Listen") }
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(state.input))
                        Toast.makeText(context, "Input copied", Toast.LENGTH_SHORT).show()
                    }, enabled = state.input.isNotBlank()) { Text("Copy") }
                }
                Button(onClick = vm::translate, modifier = Modifier.fillMaxWidth(), enabled = !state.loading && state.input.isNotBlank()) {
                    Text(if (state.loading) "Translating…" else "Translate")
                }
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Translation", fontWeight = FontWeight.SemiBold)
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.output.ifBlank { "Your translation will appear here." }, style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onSpeak(state.output, state.target) }, enabled = state.output.isNotBlank()) { Text("Listen") }
                            TextButton(onClick = {
                                clipboard.setText(AnnotatedString(state.output))
                                Toast.makeText(context, "Translation copied", Toast.LENGTH_SHORT).show()
                            }, enabled = state.output.isNotBlank()) { Text("Copy") }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, state.output)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share translation"))
                            }, enabled = state.output.isNotBlank()) { Text("Share") }
                        }
                        TextButton(onClick = vm::saveLast, enabled = state.lastId != null) {
                            Text(if (isSaved) "★ Saved" else "☆ Save translation")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePicker(selected: String, options: List<Language>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(languageName(selected), maxLines = 1, fontSize = 12.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { language ->
                DropdownMenuItem(text = { Text(language.label) }, onClick = { onSelect(language.code); expanded = false })
            }
        }
    }
}

@Composable
private fun RecordsScreen(
    modifier: Modifier, title: String, entries: List<TranslationEntity>, emptyMessage: String,
    vm: TranslatorViewModel, onSpeak: (String, String) -> Unit
) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp, bottom = 12.dp))
        if (entries.isEmpty()) {
            Card(Modifier.fillMaxWidth()) { Text(emptyMessage, Modifier.padding(20.dp)) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(entries, key = { it.id }) { entry -> RecordCard(entry, vm, onSpeak) }
            }
        }
    }
}

@Composable
private fun RecordCard(entry: TranslationEntity, vm: TranslatorViewModel, onSpeak: (String, String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(languageName(entry.sourceCode) + " → " + languageName(entry.targetCode),
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(entry.sourceText, fontWeight = FontWeight.Medium)
            Text(entry.translatedText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.createdAt)), fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(entry.translatedText))
                    Toast.makeText(context, "Translation copied", Toast.LENGTH_SHORT).show()
                }) { Text("Copy") }
                TextButton(onClick = { onSpeak(entry.translatedText, entry.targetCode) }) { Text("Listen") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { vm.toggle(entry) }) { Text(if (entry.isFavourite) "★ Saved" else "☆ Save") }
                TextButton(onClick = { vm.delete(entry) }) { Text("Delete") }
            }
        }
    }
}
