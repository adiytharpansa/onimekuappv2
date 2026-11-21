plugins {
    id("com.android.application") version "8.7.3" apply false
    // Kotlin diselaraskan ke 2.2.10: libmpv 1.2.0 dibangun dengan Kotlin itu
    // dan menyeret kotlin-stdlib 2.2.10; compiler 2.0.21 mogok membaca
    // metadata stdlib yang lebih baru.
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
}
