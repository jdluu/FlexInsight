package com.example.flexinsight.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.flexinsight.R
import androidx.compose.ui.res.stringResource
import com.example.flexinsight.ui.theme.*
import com.example.flexinsight.ui.viewmodel.SettingsViewModel

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var apiKey by remember { mutableStateOf("") }
    val isValid = apiKey.length >= 10
    val uiState by viewModel.uiState.collectAsState()

    // If API key is saved successfully, complete onboarding
    LaunchedEffect(uiState.apiKey) {
        if (!uiState.apiKey.isNullOrBlank()) {
            onComplete()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            BackgroundDark
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo/Icon Section
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(id = R.string.onboarding_welcome),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Input Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardAlt),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(id = R.string.onboarding_connect_hevy), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = stringResource(id = R.string.onboarding_api_key_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text(stringResource(id = R.string.onboarding_api_key_label)) },
                            placeholder = { Text(stringResource(id = R.string.onboarding_api_key_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Primary
                            )
                        )

                        Button(
                            onClick = { viewModel.validateAndSaveApiKey(apiKey) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = isValid,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text(stringResource(id = R.string.onboarding_get_started), fontWeight = FontWeight.Bold, color = BackgroundDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = { /* Could link to instructions */ }
                ) {
                    Text(stringResource(id = R.string.onboarding_find_key_link), color = TextSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}
