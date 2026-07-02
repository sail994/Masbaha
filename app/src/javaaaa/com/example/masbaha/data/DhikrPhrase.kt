package com.example.masbaha.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phrases_table")
data class DhikrPhrase(
    @PrimaryKey(autoGenerate = true) 
    val id: Int = 0,
    
    val text: String,
    
    val count: Int = 0
)
