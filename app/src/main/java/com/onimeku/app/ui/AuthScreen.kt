package com.onimeku.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import com.onimeku.app.Route
import com.onimeku.app.data.AuthClient

private val EmailRx = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

@Composable
private fun AuthField(
    label: String, value: String, onChange: (String) -> Unit, error: String?,
    keyboard: KeyboardType, ime: ImeAction, password: Boolean = false, onDone: () -> Unit = {},
) {
    var show by rememberSaveable { mutableStateOf(false) }
    val bd by animateColorAsState(if (error != null) RedText else Line, label = "fieldBd")
    Column(Modifier.padding(bottom = 14.dp)) {
        Text(label, fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Row(
            Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp)).background(Bg2)
                .border(1.dp, bd, RoundedCornerShape(16.dp)).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value, onChange, Modifier.weight(1f), singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp), cursorBrush = SolidColor(Color.White),
                visualTransformation = if (password && !show) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard, imeAction = ime),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }, onNext = { }),
            )
            if (password) Box(Modifier.size(40.dp).clip(CircleShape).clickable { show = !show }, contentAlignment = Alignment.Center) {
                Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (show) "Hide password" else "Show password",
                    Modifier.size(20.dp), tint = NavOff)
            }
        }
        if (error != null) Text(error, fontSize = 12.sp, color = RedText, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
    }
}

/** Halaman Sign in / Sign up (email + kata sandi). */
@Composable
fun AuthScreen(vm: AppVM, startSignUp: Boolean) {
    var signUp by rememberSaveable { mutableStateOf(startSignUp) }
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var tried by rememberSaveable { mutableStateOf(false) }

    val emailErr = if (tried && !EmailRx.matches(email.trim())) "Enter a valid email address." else null
    val passErr = if (tried && pass.length < 8) "Use at least 8 characters." else null
    val confErr = if (tried && signUp && confirm != pass) "Passwords do not match." else null

    fun submit() {
        tried = true
        if (!EmailRx.matches(email.trim()) || pass.length < 8 || (signUp && confirm != pass)) return
        if (signUp) vm.signUp(email, pass) else vm.signIn(email, pass)
    }

    Page(extraTop = 8.dp) {
        SubHead("", vm)
        Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp, bottom = 14.dp)) { Logo(64.dp) }
        Text(if (signUp) I18n.t("create_account_t", "Create your account") else I18n.t("welcome_back", "Welcome back"), Modifier.fillMaxWidth(), fontSize = 28.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            if (signUp) "Save your list and keep watching on any device." else "Sign in to sync your list and progress.",
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 26.dp), color = InkSoft, fontSize = 14.sp, textAlign = TextAlign.Center,
        )

        if (!AuthClient.configured) {
            CardBox(Modifier.padding(bottom = 18.dp)) {
                Text("Accounts are not available in this build yet. You can keep using Onimeku as a guest.",
                    Modifier.padding(16.dp), color = InkSoft, fontSize = 13.5.sp)
            }
        }

        AuthField(I18n.t("email", "Email"), email, { email = it }, emailErr, KeyboardType.Email, ImeAction.Next)
        AuthField(I18n.t("password", "Password"), pass, { pass = it }, passErr, KeyboardType.Password, if (signUp) ImeAction.Next else ImeAction.Done,
            password = true, onDone = { submit() })
        if (signUp) AuthField("Confirm password", confirm, { confirm = it }, confErr, KeyboardType.Password, ImeAction.Done,
            password = true, onDone = { submit() })

        vm.authError?.let {
            Text(it, color = RedText, fontSize = 13.5.sp, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
        }

        Box(Modifier.padding(top = 6.dp)) {
            Btn(if (vm.authBusy) I18n.t("please_wait", "Please wait…") else if (signUp) I18n.t("create_account_b", "Create account") else I18n.t("sign_in", "Sign in"), BtnStyle.Play,
                modifier = Modifier.fillMaxWidth(), height = 54.dp, loading = vm.authBusy) { submit() }
        }

        if (!signUp) {
            Text("Forgot password?", Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp).clip(RoundedCornerShape(8.dp))
                .clickable {
                    if (!EmailRx.matches(email.trim())) { tried = true; vm.toast("Enter your email first") } else vm.resetPassword(email)
                }.padding(horizontal = 10.dp, vertical = 6.dp), color = RedText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (signUp) "Already have an account? " else "New to Onimeku? ", color = InkSoft, fontSize = 14.sp)
            Text(if (signUp) I18n.t("sign_in", "Sign in") else I18n.t("sign_up", "Sign up"), color = RedText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { signUp = !signUp; tried = false; vm.authError = null }.padding(6.dp))
        }

        Text("By continuing you agree to our Terms of Service and Privacy Policy.", Modifier.fillMaxWidth().padding(top = 18.dp),
            color = NavOff, fontSize = 12.sp, textAlign = TextAlign.Center)
        Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Terms", color = RedText, fontSize = 12.5.sp, modifier = Modifier.clickable { vm.go(Route.Sub("terms")) }.padding(6.dp))
            Text("Privacy", color = RedText, fontSize = 12.5.sp, modifier = Modifier.clickable { vm.go(Route.Sub("privacy")) }.padding(6.dp))
        }
    }
}
