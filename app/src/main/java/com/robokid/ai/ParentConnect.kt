package com.robokid.ai

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest

@Composable
fun ParentConnect() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("robokid_parent", android.content.Context.MODE_PRIVATE) }
    var parentPhone by rememberSaveable { mutableStateOf(prefs.getString("phone", "").orEmpty()) }
    var pin by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var parentMessage by rememberSaveable { mutableStateOf("A parent must set up and approve this contact.") }
    var parentMode by rememberSaveable { mutableStateOf(false) }
    var messageText by rememberSaveable { mutableStateOf("Hi! I need your help.") }
    val hasPin = !prefs.getString("pin_hash", null).isNullOrBlank()
    val savedPhone = prefs.getString("phone", "").orEmpty()

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(18.dp))
        Text("👨‍👩‍👧 Talk to My Parent", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF123B72))
        Text("Reach the parent or guardian approved on this phone.", fontSize = 13.sp, color = Color(0xFF52677F))
        if (savedPhone.isBlank()) {
            Text("Parent contact is not set up yet. Ask your parent or guardian to configure it.", color = Color(0xFF9A5B00), fontSize = 13.sp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$savedPhone")))
                        parentMessage = "Phone dialler opened. Check the number and tap call."
                    } catch (_: Exception) { parentMessage = "Could not open the phone dialler." }
                }) { Text("Call parent") }
                OutlinedButton(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$savedPhone")).apply {
                            putExtra("sms_body", "Hi, I need your help.")
                        })
                        parentMessage = "SMS opened. Review and send your message."
                    } catch (_: Exception) { parentMessage = "Could not open SMS." }
                }) { Text("SMS") }
            }
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it.take(300) },
                label = { Text("Message to parent") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = {
                val digits = savedPhone.filter { it.isDigit() }
                if (digits.length < 8) {
                    parentMessage = "Ask your parent to save the number in international format, including country code."
                } else {
                    try {
                        val link = "https://wa.me/$digits?text=" + Uri.encode(messageText)
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        parentMessage = "WhatsApp chat opened. Review and tap Send. WhatsApp calling is available from that chat."
                    } catch (_: Exception) { parentMessage = "Could not open WhatsApp. Check that it is installed." }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("WhatsApp message") }
        }
        Text(parentMessage, fontSize = 12.sp, color = Color(0xFF52677F))
        Spacer(Modifier.height(10.dp))
        if (!parentMode) {
            Text("Parent setup", fontWeight = FontWeight.Bold, color = Color(0xFF123B72))
            if (hasPin) {
                OutlinedTextField(value = pin, onValueChange = { pin = it.filter(Char::isDigit).take(8) }, label = { Text("Parent PIN") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (pinHash(pin) == prefs.getString("pin_hash", null)) {
                        parentMode = true
                        parentMessage = "Parent settings unlocked."
                        pin = ""
                    } else parentMessage = "Incorrect parent PIN."
                }, modifier = Modifier.fillMaxWidth()) { Text("Unlock parent settings") }
            } else {
                Text("Create a parent PIN (4–8 digits) to protect contact settings.", fontSize = 13.sp, color = Color(0xFF52677F))
                OutlinedTextField(value = newPin, onValueChange = { newPin = it.filter(Char::isDigit).take(8) }, label = { Text("Create parent PIN") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (newPin.length in 4..8) {
                        prefs.edit().putString("pin_hash", pinHash(newPin)).apply()
                        parentMode = true
                        newPin = ""
                        parentMessage = "Parent PIN created. Now save an approved contact."
                    } else parentMessage = "PIN must contain 4 to 8 digits."
                }, modifier = Modifier.fillMaxWidth()) { Text("Create PIN and continue") }
            }
        } else {
            Text("Parent controls", fontWeight = FontWeight.Bold, color = Color(0xFF123B72))
            OutlinedTextField(value = parentPhone, onValueChange = { parentPhone = it.take(20) }, label = { Text("Parent phone (+country code)") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val digits = parentPhone.filter { it.isDigit() }
                if (digits.length < 8 || !parentPhone.trim().startsWith("+")) {
                    parentMessage = "Enter the approved number with country code, for example +27…"
                } else {
                    prefs.edit().putString("phone", parentPhone.trim()).apply()
                    parentMessage = "Approved parent number saved on this phone."
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save approved parent contact") }
            OutlinedButton(onClick = {
                prefs.edit().remove("phone").apply()
                parentPhone = ""
                parentMessage = "Parent contact cleared. Save a new approved contact."
            }, modifier = Modifier.fillMaxWidth()) { Text("Clear parent contact") }
            OutlinedButton(onClick = { parentMode = false }, modifier = Modifier.fillMaxWidth()) { Text("Lock parent settings") }
        }
        Text("Calls open the phone dialler; SMS and WhatsApp open a draft for review. RoboKid does not send messages or place calls silently. Contact details stay on this device.", fontSize = 11.sp, color = Color(0xFF64748B))
    }
}

private fun pinHash(pin: String): String =
    MessageDigest.getInstance("SHA-256").digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
