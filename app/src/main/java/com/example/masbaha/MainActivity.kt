package com.example.masbaha

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room

import com.example.masbaha.data.AppDatabase
import com.example.masbaha.data.DhikrPhrase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class Screen {
    MAIN, HISTORY
}

class MainActivity : ComponentActivity() {

    private lateinit var db: AppDatabase
    private var selectedPhrase by mutableStateOf<DhikrPhrase?>(null)
    private var phrasesList by mutableStateOf<List<DhikrPhrase>>(emptyList())
    private var showSettingsDialog by mutableStateOf(false)
    private var showHelpDialog by mutableStateOf(false) 
    
    private var currentScreen by mutableStateOf(Screen.MAIN)
    private var isDarkModeOverride by mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "masbaha-db")
            .allowMainThreadQueries()
            .build()

        initDatabaseFromXML()

        lifecycleScope.launch {
            db.phraseDao().getAllPhrases().collect { list ->
                phrasesList = list
                if (selectedPhrase == null && list.isNotEmpty()) {
                    selectedPhrase = list.first()
                } else {
                    selectedPhrase = list.find { it.id == selectedPhrase?.id }
                }
            }
        }

        setContent {
            val useDarkTheme = isDarkModeOverride ?: isSystemInDarkTheme()

            MaterialTheme(
                colorScheme = if (useDarkTheme) darkColorScheme(
                    primary = Color(0xFFBB86FC),         
                    primaryContainer = Color(0xFFFFFFFF),
                    onPrimaryContainer = Color(0xFF000000),
                    background = Color(0xFF000000),       
                    surface = Color(0xFF121212)
                ) else lightColorScheme(
                    primary = Color(0xFF6200EE),         
                    primaryContainer = Color(0xFFE1BEE7),
                    onPrimaryContainer = Color(0xFF4A148C)
                )
            ) {
                when (currentScreen) {
                    Screen.MAIN -> {
                        MasbahaScreen(
                            phrases = phrasesList,
                            selectedPhrase = selectedPhrase,
                            isDarkMode = useDarkTheme,
                            onPhraseSelected = { phrase -> selectedPhrase = phrase },
                            onManualIncrement = { handleCounterInteraction() },
                            onResetCounter = { resetCounter() },
                            onOpenSettings = { showSettingsDialog = true },
                            onOpenHelp = { showHelpDialog = true }, 
                            onToggleTheme = { isDarkModeOverride = !useDarkTheme },
                            onNavigateToHistory = { currentScreen = Screen.HISTORY }
                        )
                    }
                    Screen.HISTORY -> {
                        HistoryScreen(
                            phrases = phrasesList,
                            onBack = { currentScreen = Screen.MAIN }
                        )
                    }
                }

                if (showSettingsDialog) {
                    SettingsDialog(
                        phrases = phrasesList,
                        onDismiss = { showSettingsDialog = false },
                        onAdd = { text, target ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                db.phraseDao().insertPhrase(DhikrPhrase(text = text, targetCount = target))
                            }
                        },
                        onUpdate = { phrase, newText, newTarget ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                db.phraseDao().updatePhrase(phrase.copy(text = newText, targetCount = newTarget))
                            }
                        },
                        onDelete = { phrase ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                db.phraseDao().deletePhrase(phrase)
                            }
                            if (selectedPhrase?.id == phrase.id) {
                                selectedPhrase = null
                            }
                        }
                    )
                }

                if (showHelpDialog) {
                    HelpDialog(onDismiss = { showHelpDialog = false })
                }
            }
        }
    }

    private fun handleCounterInteraction() {
        selectedPhrase?.let { phrase ->
            if (phrase.count >= phrase.targetCount) {
                triggerTargetVibration()
                Toast.makeText(this, "تم الوصول إلى الهدف بالفعل!", Toast.LENGTH_SHORT).show()
            } else {
                triggerClickVibration()
                val nextCount = phrase.count + 1
                lifecycleScope.launch(Dispatchers.IO) { 
                    db.phraseDao().incrementCount(phrase.id) 
                }
                if (nextCount >= phrase.targetCount) {
                    triggerTargetVibration()
                }
            }
        }
    }

    private fun resetCounter() {
        selectedPhrase?.let { phrase ->
            lifecycleScope.launch(Dispatchers.IO) {
                db.phraseDao().updatePhrase(phrase.copy(count = 0))
            }
            Toast.makeText(this, "تم إعادة تصفير العداد", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (currentScreen == Screen.MAIN) {
                handleCounterInteraction()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun initDatabaseFromXML() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentList = db.phraseDao().getAllPhrases().first()
                if (currentList.isEmpty()) {
                    val xmlPhrases = resources.getStringArray(R.array.dhikr_phrases)
                    val dhikrList = xmlPhrases.map { DhikrPhrase(text = it, targetCount = 100) }
                    db.phraseDao().insertAll(dhikrList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun triggerClickVibration() {
        val vibrator = getVibratorService()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun triggerTargetVibration() {
        val vibrator = getVibratorService()
        val timings = longArrayOf(0, 350, 150, 350, 150, 350)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    private fun getVibratorService(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasbahaScreen(
    phrases: List<DhikrPhrase>,
    selectedPhrase: DhikrPhrase?,
    isDarkMode: Boolean,
    onPhraseSelected: (DhikrPhrase) -> Unit,
    onManualIncrement: () -> Unit,
    onResetCounter: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onToggleTheme: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    var showMenuOptions by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenSettings) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Réglages", tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = "المسبحة الإلكترونية",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Box {
                    IconButton(onClick = { showMenuOptions = true }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu Options", tint = MaterialTheme.colorScheme.primary)
                    }
                    DropdownMenu(
                        expanded = showMenuOptions,
                        onDismissRequest = { showMenuOptions = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isDarkMode) "الوضع النهاري" else "الوضع الليلي") },
                            onClick = {
                                onToggleTheme()
                                showMenuOptions = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("السجل والإحصائيات") },
                            onClick = {
                                onNavigateToHistory()
                                showMenuOptions = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("طريقة الاستعمال") },
                            onClick = {
                                onOpenHelp()
                                showMenuOptions = false
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "اختر الذكر :", fontSize = 16.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    val currentPercentage = selectedPhrase?.let {
                        if (it.targetCount > 0) (it.count * 100) / it.targetCount else 0
                    } ?: 0
                    val displayText = selectedPhrase?.let { "${it.text} ($currentPercentage%)" } ?: "— اختر —"

                    OutlinedTextField(
                        value = displayText,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        phrases.forEachIndexed { index, phrase ->
                            val percentage = if (phrase.targetCount > 0) (phrase.count * 100) / phrase.targetCount else 0
                            
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = "${phrase.text} ($percentage%)", 
                                        modifier = Modifier.fillMaxWidth(), 
                                        textAlign = TextAlign.Center,
                                        color = if (phrase.count >= phrase.targetCount) Color(0xFF2E7D32) else (if (isDarkMode) Color.White else Color(0xFF007AFF))
                                    ) 
                                },
                                onClick = {
                                    onPhraseSelected(phrase)
                                    expandedDropdown = false
                                }
                            )
                            if (index < phrases.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            }
                        }
                    }
                }

                selectedPhrase?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "الهدف : ${it.targetCount}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val isCompleted = selectedPhrase?.let { it.count >= it.targetCount } ?: false
                
                val counterColor = if (isCompleted) {
                    Color(0xFF2E7D32) 
                } else {
                    if (isDarkMode) Color.White else Color(0xFF007AFF) 
                }

                Text(
                    text = "${selectedPhrase?.count ?: 0} / ${selectedPhrase?.targetCount ?: 100}",
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = counterColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onManualIncrement() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "اضغط",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Button(
                onClick = onResetCounter,
                colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF007AFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(text = "إعادة تصفير العداد", fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    phrases: List<DhikrPhrase>,
    onBack: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = "سجل الأذكار والإحصائيات",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val totalTasbih = phrases.sumOf { it.count }
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "إجمالي التسبيحات", fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(text = "$totalTasbih", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Text(
                text = "التفاصيل :",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(phrases) { phrase ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = phrase.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${phrase.count} / ${phrase.targetCount}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (phrase.count >= phrase.targetCount) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    phrases: List<DhikrPhrase>,
    onDismiss: () -> Unit,
    onAdd: (String, Int) -> Unit,
    onUpdate: (DhikrPhrase, String, Int) -> Unit,
    onDelete: (DhikrPhrase) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var targetInput by remember { mutableStateOf("100") }
    var editingPhrase by remember { mutableStateOf<DhikrPhrase?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إدارة الأذكار", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("الذكر") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("العدد المستهدف") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val target = targetInput.toIntOrNull() ?: 100
                        if (textInput.isNotBlank()) {
                            val currentEditing = editingPhrase
                            if (currentEditing != null) {
                                onUpdate(currentEditing, textInput, target)
                                editingPhrase = null
                            } else {
                                onAdd(textInput, target)
                            }
                            textInput = ""
                            targetInput = "100"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editingPhrase != null) "تعديل" else "إضافة جديد")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("الأذكار الحالية :", fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(phrases) { phrase ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = phrase.text, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                editingPhrase = phrase
                                textInput = phrase.text
                                targetInput = phrase.targetCount.toString()
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Color.Gray)
                            }
                            IconButton(onClick = { onDelete(phrase) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("طريقة الاستعمال", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("• اضغط على الدائرة الكبيرة في المنتصف لزيادة العداد.", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• يمكنك أيضاً استخدام أزرار التحكم في الصوت (Volume Up / Down) لزيادة العداد دون لمس الشاشة.", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• عند الوصول إلى العدد المستهدف، سيهتز الهاتف تنبيهاً لك.", fontSize = 16.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("مفهوم") }
        }
    )
}
