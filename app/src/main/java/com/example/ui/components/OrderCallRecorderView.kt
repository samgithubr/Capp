package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CoffeeOrder
import com.example.recorder.OrderAudioParser
import com.example.recorder.RecordingState
import com.example.ui.theme.AmberRoast
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.SheetGreen
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCallRecorderView(
    recordingState: RecordingState,
    recordingDurationSeconds: Int,
    currentAmplitude: Float,
    orders: List<CoffeeOrder>,
    isPlaying: Boolean,
    playingFilePath: String?,
    currentAudioPosMs: Int,
    audioDurationMs: Int,
    onStartCallAndRecord: (String, String) -> Unit,
    onStartDirectRecord: (String) -> Unit,
    onPauseRecord: () -> Unit,
    onResumeRecord: () -> Unit,
    onStopRecordAndSave: (String, String, String?) -> Unit,
    onSimulatedOrderTest: (String, String, String) -> Unit,
    onPlayAudio: (String) -> Unit,
    onPauseAudio: () -> Unit,
    onSeekAudio: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedShop by remember { mutableStateOf("Blue Bottle Coffee") }
    var shopPhone by remember { mutableStateOf("+1 (555) 432-8765") }
    var customNotesOrSpoken by remember { mutableStateOf("") }

    val presetShops = listOf(
        Pair("Blue Bottle Coffee", "+1 (555) 432-8765"),
        Pair("Philz Coffee", "+1 (555) 789-0123"),
        Pair("Starbucks Reserve", "+1 (555) 654-3210"),
        Pair("Artisan Roasters", "+1 (555) 321-9988"),
        Pair("Local Corner Cafe", "+1 (555) 901-2345")
    )

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Privacy & Offline Notice Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = SheetGreen.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SheetGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "100% Offline & Private Local Recording",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SheetGreen
                        )
                        Text(
                            text = "Audio stays on your device storage. No paid third-party APIs or subscription fees.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active Recording Status Card (Visible when Recording or Paused)
        item {
            AnimatedVisibility(visible = recordingState != RecordingState.IDLE) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (recordingState == RecordingState.RECORDING)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )

                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = if (recordingState == RecordingState.RECORDING)
                                    Color.Red.copy(alpha = pulseAlpha)
                                else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (recordingState == RecordingState.RECORDING)
                                    "RECORDING CALL AUDIO: $selectedShop"
                                else "RECORDING PAUSED",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Duration Timer
                        val minutes = recordingDurationSeconds / 60
                        val seconds = recordingDurationSeconds % 60
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Waveform Sound Bar Simulation based on actual amplitude
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val barCount = 18
                            for (i in 0 until barCount) {
                                val factor = ((i + 1) % 4) * 0.25f
                                val barHeight = (8.dp + ((28 * currentAmplitude * factor).toInt()).dp).coerceIn(4.dp, 32.dp)
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (recordingState == RecordingState.RECORDING)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.outline
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Control Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (recordingState == RecordingState.RECORDING) {
                                OutlinedButton(
                                    onClick = onPauseRecord,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pause")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onResumeRecord,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume")
                                }
                            }

                            Button(
                                onClick = {
                                    onStopRecordAndSave(
                                        selectedShop,
                                        shopPhone,
                                        customNotesOrSpoken.ifBlank { null }
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SheetGreen),
                                modifier = Modifier.testTag("stop_and_save_recording_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop & Add to Sheet")
                            }
                        }
                    }
                }
            }
        }

        // Phone Call Coffee Shop Setup Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Call Coffee Shop to Order",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select or enter cafe phone number to record call on Android 10+",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Quick Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetShops) { (shop, phone) ->
                            val isSelected = selectedShop == shop
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedShop = shop
                                    shopPhone = phone
                                },
                                label = { Text(shop, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = selectedShop,
                        onValueChange = { selectedShop = it },
                        label = { Text("Coffee Shop Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("coffee_shop_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = shopPhone,
                        onValueChange = { shopPhone = it },
                        label = { Text("Shop Phone Number") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("coffee_shop_phone_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Call & Record Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onStartCallAndRecord(selectedShop, shopPhone) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_and_record_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown)
                        ) {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Call & Record Call", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { onStartDirectRecord(selectedShop) },
                            modifier = Modifier.testTag("record_voice_order_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberRoast)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Voice Order")
                        }
                    }
                }
            }
        }

        // Simulated Spoken Order Tester (Free testing without placing phone calls)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = AmberRoast,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test Spoken Order Parser",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Text(
                        text = "Simulate an order transcript to test spreadsheet entry without calling:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OrderAudioParser.sampleVoiceTranscripts.take(3).forEach { sample ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    onSimulatedOrderTest(sample, selectedShop, shopPhone)
                                }
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🗣️ \"$sample\"",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Recent Call Recordings with Audio Playback
        item {
            val recordedOrders = orders.filter { it.audioFilePath != null }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Saved Call Audio Recordings (${recordedOrders.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (recordedOrders.isEmpty()) {
                    Text(
                        text = "No recorded call audio yet. Record a call or voice order to view playback logs.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    recordedOrders.forEach { order ->
                        val audioPath = order.audioFilePath ?: return@forEach
                        val isThisPlaying = isPlaying && playingFilePath == audioPath

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = order.shopName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${order.formattedDate} • ${order.audioDurationSeconds}s • \$${String.format(Locale.US, "%.2f", order.totalAmount)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (isThisPlaying) onPauseAudio() else onPlayAudio(audioPath)
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(SheetGreen, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isThisPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (isThisPlaying) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val progress = if (audioDurationMs > 0)
                                        (currentAudioPosMs.toFloat() / audioDurationMs).coerceIn(0f, 1f)
                                    else 0f

                                    Slider(
                                        value = progress,
                                        onValueChange = { newPct ->
                                            val newPos = (newPct * audioDurationMs).toInt()
                                            onSeekAudio(newPos)
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = SheetGreen,
                                            activeTrackColor = SheetGreen
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%02d:%02d", currentAudioPosMs / 1000 / 60, (currentAudioPosMs / 1000) % 60),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%02d:%02d", audioDurationMs / 1000 / 60, (audioDurationMs / 1000) % 60),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
