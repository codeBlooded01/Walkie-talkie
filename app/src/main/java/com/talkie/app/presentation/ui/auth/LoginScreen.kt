package com.talkie.app.presentation.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talkie.app.presentation.AuthState
import com.talkie.app.presentation.AuthViewModel
import com.talkie.app.presentation.ui.LeagueSpartan
import com.talkie.app.presentation.ui.Montserrat
import com.talkie.app.presentation.ui.NavyBlue

// ── Theme tokens based on the new clean design ─────────────────────────────
private val White          = Color.White
private val TitleColor     = Color.Black
private val SubtitleColor  = Color(0xFF7A7A7A)
private val BorderColor    = Color(0xFFE0E0E0)
private val ButtonColor    = NavyBlue // App's color theme as requested

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Error) { /* errors shown inline */ }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        Text(
            text = "Welcome Back",
            fontFamily = Montserrat,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = TitleColor
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Hi Welcome Back, You've Been Missed.",
            fontFamily = LeagueSpartan,
            fontSize = 14.sp,
            color = SubtitleColor
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (authState is AuthState.Error) {
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                fontFamily = LeagueSpartan,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        AuthLabeledField(
            label = "Username",
            value = username,
            onValueChange = { username = it },
            placeholder = "Enter Your Username"
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthLabeledField(
            label = "Password",
            value = password,
            onValueChange = { password = it },
            placeholder = "••••••••",
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePassword = { passwordVisible = !passwordVisible }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(checkedColor = ButtonColor)
                )
                Text(
                    text = "Remember For 30 Days",
                    fontFamily = LeagueSpartan,
                    fontSize = 12.sp,
                    color = SubtitleColor
                )
            }
            Text(
                text = "Forgot Password",
                fontFamily = LeagueSpartan,
                fontSize = 12.sp,
                color = SubtitleColor,
                modifier = Modifier.clickable { /* Handle forgot password */ }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { authViewModel.login(username, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ButtonColor),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("Sign In", fontFamily = LeagueSpartan, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OrDivider()

        Spacer(modifier = Modifier.height(24.dp))

        SocialButton(text = "Sign In With Google", icon = Icons.Outlined.AccountCircle)
        Spacer(modifier = Modifier.height(12.dp))
        SocialButton(text = "Sign in with Apple", icon = Icons.Outlined.Email)

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.Center) {
            Text("Don't Have An Account? ", fontFamily = LeagueSpartan, fontSize = 14.sp, color = SubtitleColor)
            Text(
                text = "Sign Up For Free",
                fontFamily = LeagueSpartan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ButtonColor,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    authViewModel.resetState()
                    onNavigateToRegister()
                }
            )
        }
    }
}

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("FIELD_WORKER") }
    var termsAgreed by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Registered -> {
                authViewModel.resetState()
                onNavigateToLogin()
            }
            is AuthState.Error -> {
                localError = (authState as AuthState.Error).message
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "Create Account",
            fontFamily = Montserrat,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = TitleColor
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Let's create account together",
            fontFamily = LeagueSpartan,
            fontSize = 14.sp,
            color = SubtitleColor
        )

        Spacer(modifier = Modifier.height(30.dp))

        val errorMsg = if (authState is AuthState.Error) (authState as AuthState.Error).message else localError
        if (errorMsg.isNotBlank()) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                fontFamily = LeagueSpartan,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        AuthLabeledField(
            label = "Full Name",
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = "Enter Your Name"
        )
        Spacer(modifier = Modifier.height(16.dp))

        AuthLabeledField(
            label = "Username",
            value = username,
            onValueChange = { username = it },
            placeholder = "Enter Your Username"
        )
        Spacer(modifier = Modifier.height(16.dp))

        AuthLabeledField(
            label = "Password",
            value = password,
            onValueChange = { password = it },
            placeholder = "••••••••",
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePassword = { passwordVisible = !passwordVisible }
        )
        Spacer(modifier = Modifier.height(16.dp))

        AuthLabeledField(
            label = "Designation",
            value = designation,
            onValueChange = { designation = it },
            placeholder = "e.g. Patrol Unit A"
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Role", fontFamily = LeagueSpartan, fontSize = 14.sp, color = TitleColor)
            Spacer(modifier = Modifier.height(8.dp))
            RoleSelector(selectedRole = role, onRoleChange = { role = it })
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = termsAgreed,
                onCheckedChange = { termsAgreed = it },
                colors = CheckboxDefaults.colors(checkedColor = ButtonColor)
            )
            Text("Agree With ", fontFamily = LeagueSpartan, fontSize = 12.sp, color = SubtitleColor)
            Text(
                "Terms & Condition",
                fontFamily = LeagueSpartan,
                fontSize = 12.sp,
                color = ButtonColor,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { /* Show Terms */ }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                localError = ""
                if (username.isBlank() || password.isBlank() || fullName.isBlank() || designation.isBlank()) {
                    localError = "All fields are required"
                } else if (!termsAgreed) {
                    localError = "Please agree to the Terms & Condition"
                } else {
                    authViewModel.register(username, password, fullName, designation, role)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ButtonColor),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("Sign Up", fontFamily = LeagueSpartan, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        OrDivider()
        Spacer(modifier = Modifier.height(24.dp))

        SocialButton(text = "Sign Up With Google", icon = Icons.Outlined.AccountCircle)
        Spacer(modifier = Modifier.height(12.dp))
        SocialButton(text = "Sign in with Apple", icon = Icons.Outlined.Email)

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.Center) {
            Text("Already have an account? ", fontFamily = LeagueSpartan, fontSize = 14.sp, color = SubtitleColor)
            Text(
                text = "Sign In",
                fontFamily = LeagueSpartan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ButtonColor,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    authViewModel.resetState()
                    onNavigateToLogin()
                }
            )
        }
    }
}

// ── Shared UI Components matching the image ─────────────────────────────────

@Composable
private fun AuthLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontFamily = LeagueSpartan,
            fontSize = 14.sp,
            color = TitleColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    fontFamily = LeagueSpartan,
                    fontSize = 14.sp,
                    color = SubtitleColor
                )
            },
            singleLine = true,
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = if (isPassword)
                KeyboardOptions(keyboardType = KeyboardType.Password)
            else KeyboardOptions.Default,
            trailingIcon = if (isPassword && onTogglePassword != null) {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility
                                          else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = SubtitleColor
                        )
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ButtonColor,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = White,
                unfocusedContainerColor = White
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = LeagueSpartan,
                fontSize = 14.sp
            )
        )
    }
}

@Composable
private fun RoleSelector(selectedRole: String, onRoleChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf("FIELD_WORKER" to "Field Worker", "DISPATCHER" to "Dispatcher").forEach { (roleValue, label) ->
            val isSelected = selectedRole == roleValue
            OutlinedButton(
                onClick = { onRoleChange(roleValue) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) ButtonColor else Color.Transparent,
                    contentColor   = if (isSelected) White else TitleColor
                ),
                border = BorderStroke(1.dp, if (isSelected) ButtonColor else BorderColor)
            ) {
                Text(
                    text = label,
                    fontFamily = LeagueSpartan,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Divider(modifier = Modifier.weight(1f), color = BorderColor)
        Text(
            text = "Or",
            modifier = Modifier.padding(horizontal = 16.dp),
            fontFamily = LeagueSpartan,
            fontSize = 12.sp,
            color = SubtitleColor
        )
        Divider(modifier = Modifier.weight(1f), color = BorderColor)
    }
}

@Composable
private fun SocialButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OutlinedButton(
        onClick = { /* Handle Social Sign In */ },
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = White)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TitleColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontFamily = LeagueSpartan,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = TitleColor
        )
    }
}
