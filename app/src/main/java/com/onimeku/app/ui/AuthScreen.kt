package com.onimeku.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

/** Email + password lewat Firebase Auth. Butuh app/google-services.json (lihat README). */
@Composable
fun AuthScreen(signUp: Boolean, onBack: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current; val ready = FirebaseApp.getApps(ctx).isNotEmpty()
    var email by remember { mutableStateOf("") }; var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(if (ready) null else "Firebase belum dikonfigurasi: tambahkan app/google-services.json.") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        IconButton(onBack, Modifier.offset(x = (-12).dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Text(if (signUp) "Sign up" else "Sign in", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(email, { email = it.trim() }, Modifier.fillMaxWidth().padding(top = 20.dp), label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(pw, { pw = it }, Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text(if (signUp) "Password (min. 6 karakter)" else "Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        err?.let { Text(it, Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.error) }
        Button({
            busy = true; err = null; val auth = FirebaseAuth.getInstance()
            (if (signUp) auth.createUserWithEmailAndPassword(email, pw) else auth.signInWithEmailAndPassword(email, pw))
                .addOnCompleteListener { busy = false; if (it.isSuccessful) onDone() else err = it.exception?.localizedMessage }
        }, Modifier.fillMaxWidth().padding(top = 20.dp), enabled = ready && !busy && email.isNotEmpty() && pw.length >= 6) { Text(if (signUp) "Create account" else "Sign in") }
        if (!signUp) TextButton({ if (email.isNotEmpty()) FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener { err = if (it.isSuccessful) "Email reset password dikirim." else it.exception?.localizedMessage } else err = "Isi email dulu." },
            enabled = ready) { Text("Lupa password?") }
    }
}
