package com.example.masbaha

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.masbaha.speech.SpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private lateinit var db: AppDatabase
    private lateinit var speechManager: SpeechManager
    
    // État pour savoir quelle phrase est actuellement sélectionnée
    private var selectedPhrase by mutableStateOf<DhikrPhrase?>(null)
    private var phrasesList by mutableStateOf<List<DhikrPhrase>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisation Base de données
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "masbaha-db").build()

        // Charger les données depuis le fichier XML si la BDD est vide
        initDatabaseFromXML()

        // Demande de permission Microphone
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        // Initialisation de la reconnaissance vocale
        speechManager = SpeechManager(this) { spokenText ->
            handleSpokenText(spokenText)
        }

        // Observer les changements dans la base de données
        lifecycleScope.launch {
            db.phraseDao().getAllPhrases().collect { list ->
                phrasesList = list
                if (selectedPhrase == null && list.isNotEmpty()) {
                    selectedPhrase = list.first()
                } else {
                    // Mettre à jour l'état de la phrase sélectionnée pour rafraîchir le compteur
                    selectedPhrase = list.find { it.id == selectedPhrase?.id }
                }
            }
        }

        setContent {
            MasbahaScreen(
                phrases = phrasesList,
                selectedPhrase = selectedPhrase,
                onPhraseSelected = { selectedPhrase = it },
                onManualIncrement = { 
                    selectedPhrase?.let { phrase ->
                        lifecycleScope.launch(Dispatchers.IO) { db.phraseDao().incrementCount(phrase.id) }
                    }
                },
                onVoiceButtonClick = { speechManager.startListening() }
            )
        }
    }

    private fun initDatabaseFromXML() {
    lifecycleScope.launch(Dispatchers.IO) {
        // .first() prend la première valeur émise et ferme immédiatement l'écoute
        val currentList = db.phraseDao().getAllPhrases().first() 
        if (currentList.isEmpty()) {
            val xmlPhrases = resources.getStringArray(R.array.dhikr_phrases)
            val dhikrList = xmlPhrases.map { DhikrPhrase(text = it) }
            db.phraseDao().insertAll(dhikrList)
        }
    }
}

    private fun handleSpokenText(spokenText: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            // Option 1 : Incrémenter si la parole correspond exactement à une phrase de la liste
            db.phraseDao().incrementCountByText(spokenText.trim())
            
            // Option 2 (Alternative) : Incrémenter la phrase sélectionnée peu importe ce qui est dit
            // selectedPhrase?.let { db.phraseDao().incrementCount(it.id) }
        }
    }
}

@Composable
fun MasbahaScreen(
    phrases: List<DhikrPhrase>,
    selectedPhrase: DhikrPhrase?,
    onPhraseSelected: (DhikrPhrase) -> Unit,
    onManualIncrement: () -> Unit,
    onVoiceButtonClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "المسبحة الإلكترونية",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Sélecteur de phrases (Drop-down ou liste simple pour l'exemple)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "اختر الذكر :", fontSize = 16.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Affichage du texte sélectionné
                selectedPhrase?.let {
                    Text(
                        text = it.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Compteur et Bouton circulaire principal d'incrémentation
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${selectedPhrase?.count ?: 0}",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Bouton circulaire
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onManualIncrement() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "اضغط",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Bouton de commande vocale (Microphone)
            Button(
                onClick = onVoiceButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .height(56.dp)
            ) {
                Text(text = "تحدث الآن (Reconnaissance Vocale)", fontSize = 18.sp)
            }
        }
    }
}
