package com.example.orthofinixai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orthofinixai.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orthofinixai.ui.viewmodel.AuthViewModel
import com.example.orthofinixai.ui.viewmodel.AuthState

@Composable
fun SignUpScreen(
    onSignUpClick: () -> Unit,
    onSignInClick: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Authenticated) {
            onSignUpClick()
        }
    }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    
    // Validation Errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var termsError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true
        if (fullName.isBlank()) { nameError = "Name is required"; isValid = false } else nameError = null
        
        val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})".toRegex()
        if (email.isBlank() || !emailRegex.matches(email)) { emailError = "Valid email is required"; isValid = false } else emailError = null
        
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-zA-Z]).{6,}\$".toRegex()
        if (!passwordRegex.matches(password)) { 
            passwordError = "Password must be 6+ chars with letters and numbers"
            isValid = false 
        } else passwordError = null
        
        if (password != confirmPassword) { confirmError = "Passwords do not match"; isValid = false } else confirmError = null
        
        if (!termsAccepted) { termsError = "You must accept the terms"; isValid = false } else termsError = null
        
        return isValid
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen
        )
        Text(
            "Join Orthofinix.ai community",
            color = TextGray,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                
                InputField(
                    label = "Full Name", 
                    value = fullName, 
                    onValueChange = { fullName = it; nameError = null }, 
                    placeholder = "Dr. John Doe", 
                    icon = Icons.Default.Person,
                    error = nameError
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                InputField(
                    label = "Email Address", 
                    value = email, 
                    onValueChange = { email = it; emailError = null }, 
                    placeholder = "john@example.com", 
                    icon = Icons.Default.Email,
                    error = emailError
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Password", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("••••••••", color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextGray) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(image, contentDescription = null, tint = TextGray)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    isError = passwordError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        unfocusedBorderColor = BorderColor,
                        focusedBorderColor = PrimaryGreen
                    )
                )
                if (passwordError != null) {
                    Text(passwordError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                InputField(
                    label = "Confirm Password", 
                    value = confirmPassword, 
                    onValueChange = { confirmPassword = it; confirmError = null }, 
                    placeholder = "••••••••", 
                    icon = Icons.Default.Lock, 
                    isPassword = true,
                    error = confirmError
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it; termsError = null },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                    )
                    Text("I accept the Terms and Conditions", fontSize = 14.sp, color = TextGray)
                }
                if (termsError != null) {
                    Text(termsError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Error / Info message display
                when (val state = uiState) {
                    is AuthState.Error -> {
                        Text(
                            state.message,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    else -> {}
                }

                Button(
                    onClick = { if (validate()) viewModel.signUp(email, password, fullName) },
                    enabled = uiState !is AuthState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    if (uiState is AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", color = TextGray)
            TextButton(onClick = onSignInClick) {
                Text("Sign in", color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    error: String? = null
) {
    Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextGray) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = TextGray) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(12.dp),
        isError = error != null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            unfocusedBorderColor = BorderColor,
            focusedBorderColor = PrimaryGreen
        )
    )
    if (error != null) {
        Text(error, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
