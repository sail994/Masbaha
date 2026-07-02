plugins {
    // Plugin Android Application standard
    id("com.android.application") version "8.3.2" apply false
    
    // Plugin Kotlin pour Android
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    
    // Plugin KSP (Kotlin Symbol Processing) - Requis pour compiler la base de données Room efficacement
    id("com.google.devtools.ksp") version "1.9.23-1.0.20" apply false
}
