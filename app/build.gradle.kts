plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("com.google.devtools.ksp"); id("org.jetbrains.kotlin.plugin.serialization") }

val apiUrl = (findProperty("ONIMEKU_API_URL") as String?).orEmpty()   // contoh: https://api.domainanda.com/v1/
android {
    namespace = "com.onimeku.app"; compileSdk = 34
    defaultConfig { applicationId = "com.onimeku.app"; minSdk = 24; targetSdk = 34; versionCode = 1; versionName = "1.0.0" }
    signingConfigs { create("release") {
        // Isi via ~/.gradle/gradle.properties: ONIMEKU_STORE_FILE, ONIMEKU_STORE_PASS, ONIMEKU_KEY_ALIAS
        (findProperty("ONIMEKU_STORE_FILE") as String?)?.let { storeFile = file(it) }
        storePassword = findProperty("ONIMEKU_STORE_PASS") as String?
        keyAlias = findProperty("ONIMEKU_KEY_ALIAS") as String?
        keyPassword = findProperty("ONIMEKU_STORE_PASS") as String? } }
    buildTypes {
        debug { buildConfigField("String", "API_URL", "\"${apiUrl.ifBlank { "https://example.invalid/" }}\"") }
        release { buildConfigField("String", "API_URL", "\"$apiUrl\"")
            if (apiUrl.isBlank() && gradle.startParameter.taskNames.any { it.contains("Release", true) })
                throw GradleException("Set ONIMEKU_API_URL di gradle.properties sebelum build release.")
            isMinifyEnabled = true; isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("release") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}
dependencies {
    val media3 = "1.3.1"
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3"); implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.0"); implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.media3:media3-exoplayer:$media3"); implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-ui:$media3"); implementation("androidx.media3:media3-session:$media3")
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.coil-kt:coil-compose:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.room:room-ktx:2.6.1"); ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}

// Firebase aktif hanya bila app/google-services.json ada
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")
