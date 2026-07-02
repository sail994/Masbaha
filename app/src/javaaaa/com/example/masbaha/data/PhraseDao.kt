package com.example.masbaha.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhraseDao {

    // Récupère toutes les phrases et observe les changements en temps réel grâce au Flow
    @Query("SELECT * FROM phrases_table")
    fun getAllPhrases(): Flow<List<DhikrPhrase>>

    // Insère la liste initiale des phrases (générée depuis le XML)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(phrases: List<DhikrPhrase>)

    // Incrémente le compteur via le clic sur le bouton circulaire (par ID)
    @Query("UPDATE phrases_table SET count = count + 1 WHERE id = :id")
    suspend fun incrementCount(id: Int)

    // Incrémente le compteur via la reconnaissance vocale (par correspondance de texte)
    @Query("UPDATE phrases_table SET count = count + 1 WHERE text = :text")
    suspend fun incrementCountByText(text: String)
}
