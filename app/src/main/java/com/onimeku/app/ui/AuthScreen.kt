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
import androidx.compose.ui.draw.alpha
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
    Column(Modifier.padding(bottom = Sp.S16)) {
        Text(label, fontSize = Ty.Small, color = InkSoft, modifier = Modifier.padding(start = Sp.S4, bottom = Sp.S8))
        Row(
            Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp)).background(Bg2)
                .border(1.dp, bd, RoundedCornerShape(16.dp)).padding(start = Sp.S16, end = Sp.S8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value, onChange, Modifier.weight(1f), singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = Ty.Subtitle), cursorBrush = SolidColor(Color.White),
                visualTransformation = if (password && !show) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard, imeAction = ime),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }, onNext = { }),
            )
            if (password) Box(Modifier.size(48.dp).clip(CircleShape).clickable { show = !show }, contentAlignment = Alignment.Center) {
                Icon(if (show) AppIcons.EyeOff else AppIcons.Eye, if (show) "Hide password" else "Show password",
                    Modifier.size(20.dp), tint = NavOff)
            }
        }
        if (error != null) Text(error, fontSize = Ty.Caption, color = RedText, modifier = Modifier.padding(start = Sp.S4, top = Sp.S4))
    }
}

/** Halaman Sign in / Sign up (email + kata sandi, plus Google dan Facebook). */
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
        Box(Modifier.align(Alignment.CenterHorizontally).padding(top = Sp.S4, bottom = Sp.S16)) { Logo(64.dp) }
        Text(if (signUp) I18n.t("create_account_t", "Create your account") else I18n.t("welcome_back", "Welcome back"), Modifier.fillMaxWidth(), fontSize = Ty.Display,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Sp.S28))

        if (!AuthClient.configured) {
            CardBox(Modifier.padding(bottom = Sp.S16)) {
                Text("Accounts aren't available yet.",
                    Modifier.padding(Sp.S16), color = InkSoft, fontSize = Ty.Body)
            }
        }

        AuthField(I18n.t("email", "Email"), email, { email = it }, emailErr, KeyboardType.Email, ImeAction.Next)
        AuthField(I18n.t("password", "Password"), pass, { pass = it }, passErr, KeyboardType.Password, if (signUp) ImeAction.Next else ImeAction.Done,
            password = true, onDone = { submit() })
        if (signUp) AuthField("Confirm password", confirm, { confirm = it }, confErr, KeyboardType.Password, ImeAction.Done,
            password = true, onDone = { submit() })

        vm.authError?.let {
            Text(it, color = RedText, fontSize = Ty.Body, modifier = Modifier.padding(start = Sp.S4, bottom = Sp.S12))
        }

        if (!signUp) {
            Text(I18n.t("forgot_password", "Forgot password?"), Modifier.align(Alignment.End).offset(y = (-6).dp).clip(RoundedCornerShape(8.dp))
                .clickable {
                    if (!EmailRx.matches(email.trim())) { tried = true; vm.toast("Enter your email first") } else vm.resetPassword(email)
                }.padding(horizontal = Sp.S8, vertical = Sp.S8), color = RedText, fontSize = Ty.Body, fontWeight = FontWeight.Medium)
        }

        Box(Modifier.padding(top = if (signUp) Sp.S8 else Sp.S2)) {
            Btn(if (vm.authBusy) I18n.t("please_wait", "Please wait…") else if (signUp) I18n.t("create_account_b", "Create account") else I18n.t("sign_in", "Sign in"), BtnStyle.Play,
                modifier = Modifier.fillMaxWidth(), height = 54.dp, loading = vm.authBusy) { submit() }
        }

        // Pemisah + masuk/daftar dengan akun sosial (tepat di bawah tombol utama).
        Row(Modifier.fillMaxWidth().padding(top = Sp.S24, bottom = Sp.S16), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(Line))
            Text(I18n.t("or", "or"), Modifier.padding(horizontal = Sp.S16), color = NavOff, fontSize = Ty.Small)
            Box(Modifier.weight(1f).height(1.dp).background(Line))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Sp.S12)) {
            SocialButton("Google", Modifier.weight(1f), enabled = !vm.authBusy, icon = { GoogleIcon(20.dp) }) { vm.socialSignIn("google") }
            SocialButton("Facebook", Modifier.weight(1f), enabled = !vm.authBusy, icon = { FacebookIcon(20.dp) }) { vm.socialSignIn("facebook") }
        }

        Row(Modifier.align(Alignment.CenterHorizontally).padding(top = Sp.S28), verticalAlignment = Alignment.CenterVertically) {
            Text(if (signUp) "Already have an account? " else "New to Onimeku? ", color = InkSoft, fontSize = Ty.Body)
            Text(if (signUp) I18n.t("sign_in", "Sign in") else I18n.t("sign_up", "Sign up"), color = RedText, fontSize = Ty.Body, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { signUp = !signUp; tried = false; vm.authError = null }.padding(Sp.S8))
        }

        Row(Modifier.align(Alignment.CenterHorizontally).padding(top = Sp.S12, bottom = Sp.S8), horizontalArrangement = Arrangement.spacedBy(Sp.S20)) {
            Text("Terms", color = RedText, fontSize = Ty.Small, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.go(Route.Sub("terms")) }.padding(Sp.S8))
            Text("Privacy", color = RedText, fontSize = Ty.Small, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.go(Route.Sub("privacy")) }.padding(Sp.S8))
        }
    }
}

/** Tombol akun sosial: pil gelap tipis berbingkai, ikon merek + nama, tinggi sama dengan tombol utama. */
@Composable
private fun SocialButton(label: String, modifier: Modifier, enabled: Boolean, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        modifier.height(52.dp).clip(CircleShape).background(Bg2).border(1.dp, Line, CircleShape)
            .alpha(if (enabled) 1f else 0.5f)
            .pressable { if (enabled) onClick() }.padding(horizontal = Sp.S16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Sp.S12, Alignment.CenterHorizontally),
    ) {
        icon()
        Text(label, color = Color.White, fontSize = Ty.Subtitle, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
