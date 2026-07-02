package com.example.masbaha.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PhraseDao {
    @Query("SELECT * FROM dhikr_phrases")
    fun getAllPhrases(): Flow<List<DhikrPhrase>>

    @Query("UPDATE dhikr_phrases SET count = count + 1 WHERE id = :phraseId")
    fun incrementCount(phraseId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPhrase(phrase: DhikrPhrase)

    @Update
    fun updatePhrase(phrase: DhikrPhrase)

    @Delete
    fun deletePhrase(phrase: DhikrPhrase)

    @Insert
    fun insertAll(phrases: List<DhikrPhrase>)
}
