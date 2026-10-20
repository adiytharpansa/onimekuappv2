plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.onimeku.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.onimeku.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        // Aplikasi hanya punya bahasa en + id: buang string terjemahan 80+ bahasa dari Compose/Material/ML Kit (APK lebih kecil).
        resourceConfigurations += listOf("en", "id")
        // Kunci publik Supabase untuk Sign in/Sign up (isi di gradle.properties; kosong = fitur akun nonaktif anggun)
        fun prop(n: String) = (project.findProperty(n) as String?)?.replace("\"", "") ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"${prop("ONIMEKU_SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${prop("ONIMEKU_SUPABASE_KEY")}\"")
        // Server terjemahan subtitle bersama (server/README-translate.md). Kosong = hanya ML Kit di HP.
        buildConfigField("String", "TR_URL", "\"${prop("ONIMEKU_TR_URL")}\"")
        buildConfigField("String", "TR_KEY", "\"${prop("ONIMEKU_TR_KEY")}\"")
    }
    // Tanda tangan rilis: aktif hanya bila ONIMEKU_STORE_FILE diberikan (gradle.properties / -P / CI).
    signingConfigs {
        create("release") {
            val f = project.findProperty("ONIMEKU_STORE_FILE") as String?
            if (!f.isNullOrBlank()) {
                storeFile = file(f)
                storePassword = project.findProperty("ONIMEKU_STORE_PASS") as String?
                keyAlias = project.findProperty("ONIMEKU_KEY_ALIAS") as String?
                keyPassword = (project.findProperty("ONIMEKU_KEY_PASS") as String?) ?: storePassword
            }
        }
    }
    buildTypes {
        release {
            if (!(project.findProperty("ONIMEKU_STORE_FILE") as String?).isNullOrBlank()) signingConfig = signingConfigs.getByName("release")
            // R8: kode lebih kecil + start lebih cepat. Aturan keep ada di proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // Buang pemeriksaan null runtime pada parameter/receiver (kode internal): kode lebih kecil dan sedikit lebih cepat.
        freeCompilerArgs += listOf("-Xno-param-assertions", "-Xno-call-assertions", "-Xno-receiver-assertions")
    }
    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/DEPENDENCIES", "/META-INF/LICENSE*", "/META-INF/NOTICE*")
    }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.0")
    implementation("androidx.media3:media3-database:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("androidx.media3:media3-session:1.5.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.media3:media3-datasource-okhttp:1.5.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    // Memasang Baseline Profile (src/main/baseline-prof.txt) saat instalasi: start dan scroll pertama lebih mulus.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    testImplementation("junit:junit:4.13.2")
}
