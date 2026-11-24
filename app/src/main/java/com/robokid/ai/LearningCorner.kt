package com.robokid.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LearningCorner() {
    var subject by rememberSaveable { mutableStateOf("Choose a lesson") }
    var question by rememberSaveable { mutableStateOf(0) }
    var answered by rememberSaveable { mutableStateOf(false) }

    val lessons = mapOf(
        "Maths" to listOf(
            "What is 2 + 3?\nThink: start at 2 and count 3 more.", 
            "What is 5 - 2?\nThink: count backwards two steps.",
            "Which is bigger: 8 or 6?\nCompare how many objects each number represents."
        ),
        "Reading" to listOf(
            "Let's practise sounds! The word SUN starts with the letter S. Can you say the sound /s/?",
            "Read this sentence: The cat is soft. Which animal is in the sentence?",
            "A noun names a person, place or thing. In 'The dog runs', which word names an animal?"
        ),
        "Science" to listOf(
            "Plants need sunlight, water and air to grow. What do you give a plant to drink?",
            "The Sun gives Earth light and warmth. Is the Sun a star or a planet?",
            "Ice is frozen water. What happens when ice gets warm?"
        )
    )
    val current = lessons[subject]?.getOrNull(question)

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        Text("📚 RoboKid Learning Corner", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF123B72))
        Text("Choose a subject and learn one small step at a time.", fontSize = 13.sp, color = Color(0xFF52677F))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Maths", "Reading", "Science").forEach { item ->
                OutlinedButton(onClick = { subject = item; question = 0; answered = false }) {
                    Text(item)
                }
            }
        }
        if (current != null) {
            Spacer(Modifier.height(10.dp))
            Text(" $subject • Lesson ${question + 1}", fontWeight = FontWeight.Bold, color = Color(0xFF123B72))
            Text(current, fontSize = 16.sp, color = Color(0xFF123B72))
            Spacer(Modifier.height(8.dp))
            Button(onClick = { answered = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (answered) "Great effort! Here's a hint" else "Show me a hint")
            }
            if (answered) {
                val hint = when (subject) {
                    "Maths" -> listOf("Answer: 5. Count 3, 4, 5.", "Answer: 3. Count back: 4, 3.", "Answer: 8 is bigger than 6.")[question]
                    "Reading" -> listOf("S makes the /s/ sound, like a snake.", "The cat is the animal.", "Dog is the noun.")[question]
                    else -> listOf("Plants need water.", "The Sun is a star.", "Ice melts into liquid water.")[question]
                }
                Text(hint, fontSize = 15.sp, color = Color(0xFF16845B), fontWeight = FontWeight.Medium)
                Text("You can ask a parent or teacher to learn more.", fontSize = 12.sp, color = Color(0xFF52677F))
                OutlinedButton(onClick = { question = (question + 1) % 3; answered = false }) {
                    Text("Next lesson")
                }
            }
        }
    }
}
