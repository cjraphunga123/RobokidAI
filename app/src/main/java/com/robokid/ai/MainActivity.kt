package com.robokid.ai

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RoboKidHome()
            }
        }
    }
}

@Composable
private fun RoboKidHome() {
    val context = LocalContext.current
    var childName by rememberSaveable { mutableStateOf("") }
    var greeted by rememberSaveable { mutableStateOf(false) }
    var speechReady by remember { mutableStateOf(false) }

    val speechEngine = remember(context) {
        TextToSpeech(context) { status ->
            speechReady = status == TextToSpeech.SUCCESS

        }
    }

    DisposableEffect(speechEngine) {
        onDispose {
            speechEngine.stop()
            speechEngine.shutdown()
        }
    }

    LaunchedEffect(speechReady, greeted, childName) {
        if (speechReady && greeted && childName.isNotBlank()) {
            speechEngine.language = Locale.getDefault()
            speechEngine.setSpeechRate(0.9f)
            speechEngine.setPitch(1.05f)
            speechEngine.speak(
                "Hello, $childName! I'm RoboKid, your friendly robot. What can I help you with today?",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "robokid-greeting"
            )
        }
    }

    val blue = Color(0xFF1769D2)
    val darkBlue = Color(0xFF123B72)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFEAF5FF), Color.White)))
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🤖", fontSize = 88.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "RoboKid AI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = darkBlue
            )
            Text(
                "Your friendly learning companion",
                color = Color(0xFF52677F),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            if (!greeted) {
                Text(
                    "What's your name?",
                    style = MaterialTheme.typography.titleLarge,
                    color = darkBlue
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = childName,
                    onValueChange = { childName = it.take(24) },
                    label = { Text("Your first name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { if (childName.isNotBlank()) greeted = true },
                    enabled = childName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = blue)
                ) {
                    Text("Meet my robot", fontSize = 17.sp)
                }
            } else {
                Text(
                    "Hello, $childName! 👋",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkBlue,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "I'm RoboKid, your friendly robot. What can I help you with today?",
                    fontSize = 19.sp,
                    lineHeight = 27.sp,
                    textAlign = TextAlign.Center,
                    color = darkBlue
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (speechReady) {
                            speechEngine.speak(
                                "Hello, $childName! I'm RoboKid, your friendly robot. What can I help you with today?",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "robokid-repeat-greeting"
                            )
                        }
                    },
                    enabled = speechReady,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = blue)
                ) {
                    Text(if (speechReady) "🔊  Hear RoboKid speak" else "Preparing robot voice…", fontSize = 18.sp)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        speechEngine.stop()
                        greeted = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE0ECFA),
                        contentColor = darkBlue
                    )
                ) {
                    Text("Change name")
                }
            }

            Spacer(Modifier.height(36.dp))
            Text(
                "A safe, parent-guided space to learn and explore.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}
