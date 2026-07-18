package com.blackamp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.blackamp.PlayerState
import com.blackamp.data.Track
import com.blackamp.ui.theme.*
import kotlin.math.absoluteValue
import kotlin.random.Random

@Composable
fun PlayerScreen(
    state: PlayerState,
    queueSize: Int,
    queueIndex: Int,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // ---------- the Winamp-style panel ----------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(PanelBrush)
                .border(1.dp, BevelHi, RoundedCornerShape(6.dp))
        ) {
            TitleBar()
            Column(Modifier.padding(10.dp)) {
                LcdDisplay(state, queueIndex, queueSize)
                Spacer(Modifier.height(8.dp))
                Visualizer(state.isPlaying)
                Spacer(Modifier.height(10.dp))
                SeekBar(state, onSeek)
                Spacer(Modifier.height(4.dp))
                Transport(
                    state, onPlayPause, onNext, onPrev, onStop, onShuffle, onRepeat
                )
            }
        }
    }
}

@Composable
private fun TitleBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(TitleBrush)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "BLACKAMP",
            color = Accent,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.weight(1f))
        repeat(3) {
            Box(
                Modifier
                    .padding(start = 3.dp)
                    .size(10.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF444444), Color(0xFF1A1A1A))))
                    .border(1.dp, Color.Black)
            )
        }
    }
}

@Composable
private fun LcdDisplay(state: PlayerState, index: Int, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(ScreenBlack)
            .border(1.dp, BevelHi, RoundedCornerShape(3.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = fmt(state.position),
            color = LcdGreen,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = state.current?.let { "${it.artist} - ${it.title}" } ?: "— no track loaded —",
                color = Accent,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetaText(if (total > 0) "${index + 1} / $total" else "-- / --")
                MetaText(if (state.duration > 0) fmt(state.duration) else "--:--")
                MetaText(if (state.isPlaying) "playing" else "paused")
            }
        }
    }
}

@Composable
private fun MetaText(s: String) {
    Text(s, color = LcdGreenDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
}

/** Decorative spectrum bars with falling peak caps, like the web skin. */
@Composable
private fun Visualizer(playing: Boolean) {
    val bars = 24
    val transition = rememberInfiniteTransition(label = "viz")
    val tick by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing)),
        label = "tick"
    )
    // seeded per-bar heights that shuffle as `tick` advances
    val heights = remember(playing) { MutableList(bars) { 0.1f } }
    if (playing) {
        val seed = (tick * 1000).toInt()
        for (i in 0 until bars) {
            val r = Random(seed / 60 * 31 + i * 17)
            heights[i] = 0.12f + r.nextFloat() * 0.85f
        }
    } else {
        for (i in 0 until bars) heights[i] = 0.04f
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(ScreenBlack)
            .border(1.dp, BevelHi, RoundedCornerShape(3.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0 until bars) {
            val h by animateFloatAsState(
                targetValue = heights[i],
                animationSpec = tween(160),
                label = "bar$i"
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(h.coerceIn(0.03f, 1f))
                    .background(
                        Brush.verticalGradient(listOf(Accent, Color(0xFF006B2E)))
                    )
            )
        }
    }
}

@Composable
private fun SeekBar(state: PlayerState, onSeek: (Long) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var temp by remember { mutableStateOf(0f) }

    val progress = if (state.duration > 0)
        (state.position.toFloat() / state.duration).coerceIn(0f, 1f) else 0f

    Slider(
        value = if (dragging) temp else progress,
        onValueChange = { dragging = true; temp = it },
        onValueChangeFinished = {
            dragging = false
            if (state.duration > 0) onSeek((temp * state.duration).toLong())
        },
        colors = SliderDefaults.colors(
            thumbColor = Color(0xFF888888),
            activeTrackColor = LcdGreenDim,
            inactiveTrackColor = Color.Black
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Transport(
    state: PlayerState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onStop: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WinButton(Icons.Filled.SkipPrevious, "Previous", onClick = onPrev, modifier = Modifier.weight(1f))
        WinButton(
            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            "Play/Pause", onClick = onPlayPause, modifier = Modifier.weight(1f)
        )
        WinButton(Icons.Filled.Stop, "Stop", onClick = onStop, modifier = Modifier.weight(1f))
        WinButton(Icons.Filled.SkipNext, "Next", onClick = onNext, modifier = Modifier.weight(1f))
        WinButton(
            Icons.Filled.Shuffle, "Shuffle", onClick = onShuffle,
            active = state.shuffle, modifier = Modifier.weight(1f)
        )
        WinButton(
            if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            "Repeat", onClick = onRepeat,
            active = state.repeatMode != Player.REPEAT_MODE_OFF,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WinButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit,
    active: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF262626), Color(0xFF0D0D0D))))
            .border(1.dp, if (active) Accent else Color.Black, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, desc, tint = if (active) Accent else Color(0xFFB9B9B9))
        }
    }
}

fun fmt(ms: Long): String {
    if (ms <= 0) return "00:00"
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%02d:%02d".format(m.absoluteValue, s.absoluteValue)
}
