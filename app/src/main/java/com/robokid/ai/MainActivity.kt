package com.robokid.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
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
    var speechStatus by remember { mutableStateOf("Preparing RoboKid's voice…") }
    var speechEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var heardWords by rememberSaveable { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var listeningStatus by remember { mutableStateOf("Tap the microphone and say hello!") }

    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(recognizer) {
        if (recognizer != null) {
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                    listeningStatus = "I'm listening…"
                }
                override fun onBeginningOfSpeech() {
                    listeningStatus = "I can hear you…"
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    listening = false
                    listeningStatus = "Working on your words…"
                }
                override fun onError(error: Int) {
                    listening = false
                    listeningStatus = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that. Please try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything. Tap the microphone and try again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed."
                        else -> "Listening stopped. Please try again."
                    }
                }
                override fun onResults(results: Bundle?) {
                    listening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    heardWords = matches?.firstOrNull().orEmpty()
                    if (heardWords.isNotBlank()) {
                        listeningStatus = "I heard you! RoboKid is thinking…"
                        val reply = createChildFriendlyReply(heardWords)
                        val engine = speechEngine
                        if (engine != null && speechReady) {
                            engine.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "robokid-reply")
                            listeningStatus = "RoboKid is replying…"
                        } else {
                            listeningStatus = "I heard you, but the robot voice is not ready yet."
                        }
                    } else {
                        listeningStatus = "I didn't catch that. Please try again."
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!partial.isNullOrBlank()) heardWords = partial
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        onDispose {
            recognizer?.destroy()
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRoboKidListening(recognizer) { listeningStatus = it }
        } else {
            listening = false
            listeningStatus = "Microphone permission was not allowed. Ask a parent to enable it in Settings."
        }
    }

    DisposableEffect(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val languageResult = engine.setLanguage(Locale.US)
                if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                    languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    speechReady = false
                    speechStatus = "English voice data is unavailable. Install an English voice in Text-to-speech settings."
                } else {
                    engine.setSpeechRate(0.88f)
                    engine.setPitch(1.0f)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            speechStatus = "RoboKid is speaking…"
                        }
                        override fun onDone(utteranceId: String?) {
                            speechStatus = "Ready to talk!"
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            speechStatus = "Voice playback failed. Check media volume."
                        }
                        override fun onError(utteranceId: String?, errorCode: Int) {
                            speechStatus = "Voice playback error ($errorCode). Check media volume."
                        }
                    })
                    speechEngine = engine
                    speechReady = true
                    speechStatus = "RoboKid's voice is ready."
                }
            } else {
                speechReady = false
                speechStatus = "Voice could not start. Check Android Text-to-speech settings."
            }
        }
        onDispose {
            speechReady = false
            speechEngine = null
            engine.stop()
            engine.shutdown()
        }
    }

    val blue = Color(0xFF1769D2)
    val darkBlue = Color(0xFF123B72)

    fun speakGreeting() {
        val engine = speechEngine
        if (!speechReady || engine == null) {
            speechStatus = "RoboKid's voice is still preparing. Please wait and try again."
            return
        }
        val message = "Hello, ${childName.trim()}! I'm RoboKid, your friendly robot. What can I help you with today?"
        val result = engine.speak(message, TextToSpeech.QUEUE_FLUSH, null, "robokid-greeting")
        if (result == TextToSpeech.ERROR) {
            speechStatus = "I couldn't play the voice. Check your phone's media volume."
        } else {
            speechStatus = "Sending greeting to the speaker…"
        }
    }

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
            Text("RoboKid AI", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = darkBlue)
            Text("Your friendly learning companion", color = Color(0xFF52677F), textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))

            if (!greeted) {
                Text("What's your name?", style = MaterialTheme.typography.titleLarge, color = darkBlue)
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
                Text("Hello, ${childName.trim()}! 👋", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = darkBlue, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    "I'm RoboKid, your friendly robot. What can I help you with today?",
                    fontSize = 19.sp,
                    lineHeight = 27.sp,
                    textAlign = TextAlign.Center,
                    color = darkBlue
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { speakGreeting() },
                    enabled = speechReady,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = blue)
                ) {
                    Text(if (speechReady) "🔊  Hear RoboKid speak" else "Preparing robot voice…", fontSize = 17.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(speechStatus, fontSize = 12.sp, color = Color(0xFF52677F), textAlign = TextAlign.Center)

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (recognizer == null) {
                            listeningStatus = "Speech recognition is not available on this phone."
                        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startRoboKidListening(recognizer) { listeningStatus = it }
                        } else {
                            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    enabled = !listening,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16845B))
                ) {
                    Text(if (listening) "🎤  Listening…" else "🎤  Talk to RoboKid", fontSize = 18.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(listeningStatus, fontSize = 13.sp, color = Color(0xFF52677F), textAlign = TextAlign.Center)
                if (heardWords.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text("RoboKid heard:", fontWeight = FontWeight.Bold, color = darkBlue)
                    Text(heardWords, fontSize = 18.sp, color = darkBlue, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        recognizer?.cancel()
                        speechEngine?.stop()
                        greeted = false
                        heardWords = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0ECFA), contentColor = darkBlue)
                ) {
                    Text("Change name")
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "A safe, parent-guided space to learn and explore.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun createChildFriendlyReply(input: String): String {
    val words = input.lowercase(Locale.ROOT)
    return when {
        words.contains("hello") || words.contains("hi") || words.contains("hey") ->
            "Hello, friend! I'm happy to talk with you. What would you like to learn today?"
        words.contains("your name") || words.contains("who are you") ->
            "I'm RoboKid, your friendly learning robot. I can help you explore ideas and learn new things."
        words.contains("thank") ->
            "You're very welcome! It's fun learning with you."
        words.contains("joke") ->
            "Why did the robot go on holiday? It needed to recharge! Would you like another joke?"
        words.contains("animal") || words.contains("dog") || words.contains("cat") ->
            "Animals are amazing! Dogs can learn many commands, and cats use their whiskers to sense spaces."
        words.contains("math") || words.contains("add") || words.contains("plus") ->
            "Let's learn together! Tell me the numbers in your question and we can work them out step by step."
        words.contains("sad") || words.contains("scared") || words.contains("afraid") ->
            "I'm sorry you're feeling that way. Please talk to a parent or trusted grown-up too. I'm here to listen."
        else ->
            "That's interesting! Can you tell me a little more? We can explore it together."
    }
}

private fun startRoboKidListening(
    recognizer: SpeechRecognizer?,
    updateStatus: (String) -> Unit
) {
    if (recognizer == null) {
        updateStatus("Speech recognition is not available on this phone.")
        return
    }
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello! What would you like to say?")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }
    try {
        recognizer.startListening(intent)
        updateStatus("Starting microphone…")
    } catch (error: Exception) {
        updateStatus("I couldn't start listening. Please try again.")
    }
}
