-keep class androidx.media3.** { *; }
-keepattributes Signature, *Annotation*, InnerClasses
-dontwarn org.slf4j.**
-keep,includedescriptorclasses class com.onimeku.app.**$$serializer { *; }
-keepclassmembers class com.onimeku.app.** { *** Companion; }
-keepclasseswithmembers class com.onimeku.app.** { kotlinx.serialization.KSerializer serializer(...); }

# --- tambahan R8 (rilis) ---
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**
-keep class com.onimeku.app.data.** { *; }          # model JSON + state tersimpan
-keep class com.onimeku.app.BuildConfig { *; }
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepattributes SourceFile, LineNumberTable          # stack trace crash tetap terbaca
-renamesourcefileattribute SourceFile
-dontwarn com.google.errorprone.annotations.**   # Tink (security-crypto)
-dontwarn javax.annotation.**
