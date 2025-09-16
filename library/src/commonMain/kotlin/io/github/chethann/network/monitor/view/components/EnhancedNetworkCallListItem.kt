package io.github.chethann.network.monitor.view.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.chethann.network.monitor.db.NetworkCallEntity
import io.github.chethann.network.monitor.view.theme.extendedColors
import io.github.chethann.network.monitor.view.TimeDisplayUtils

// Deprecated hard-coded status colors replaced by theme tokens
private val LegacyInProgress = Color(0xFFF0AD4E)
private val LegacySuccess = Color(0xFF5CB85C)
private val LegacyError = Color(0xFFD9534F)

@Composable
fun EnhancedNetworkCallListItem(
    item: NetworkCallEntity,
    onItemClick: (NetworkCallEntity) -> Unit,
    searchQuery: String = "",
    isHighlighted: Boolean = false
) {
    val colors = MaterialTheme.extendedColors
    val statusColor = when {
        item.inProgress -> colors.pending
        item.isSuccess -> colors.success
        else -> MaterialTheme.colors.error
    }

    val cardElevation = if (isHighlighted) 8.dp else 2.dp
    val borderColor = if (isHighlighted) MaterialTheme.colors.primary else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onItemClick(item) },
    backgroundColor = MaterialTheme.colors.surface,
        elevation = cardElevation,
        shape = RoundedCornerShape(8.dp),
        border = if (isHighlighted) BorderStroke(2.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            // Status indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                // Status code with background
                Card(
                    backgroundColor = statusColor,
                    shape = RoundedCornerShape(6.dp),
                    elevation = 0.dp,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = item.status?.toString() ?: "---",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // HTTP method
                Text(
                    text = item.httpMethod,
                    color = getMethodColor(item.httpMethod),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            // Main content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // URL with highlighting
                HighlightableText(
                    text = item.relativeUrl,
                    searchQuery = searchQuery,
                    highlightColor = Color(0xFFFFEB3B),
                    textColor = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Host with highlighting
                HighlightableText(
                    text = item.host,
                    searchQuery = searchQuery,
                    highlightColor = Color(0xFFFFEB3B),
                    textColor = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Time and size info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Time
                    Row {
                        Text("🕐 ", fontSize = 10.sp)
                        Text(
                            TimeDisplayUtils.getReadableTime(item.requestTimestamp),
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }

                    // Response size
                    item.responseSize?.let { size ->
                        Row {
                            Text("📦 ", fontSize = 10.sp)
                            Text(
                                size,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Duration
                    if (item.responseTimestamp != 0L && item.requestTimestamp != 0L) {
                        val duration = item.responseTimestamp - item.requestTimestamp
                        Row {
                            Text("⚡ ", fontSize = 10.sp)
                            Text(
                                "${duration}ms",
                                color = getDurationColor(duration),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Status icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        item.inProgress -> "🔄"
                        item.isSuccess -> "✅"
                        else -> "❌"
                    },
                    fontSize = 16.sp
                )
            }
        }
    }
}

// Helper function to get method colors
private fun getMethodColor(method: String): Color {
    return when (method.uppercase()) {
        "GET" -> Color(0xFF4CAF50)      // Green
        "POST" -> Color(0xFF2196F3)     // Blue
        "PUT" -> Color(0xFFFF9800)      // Orange
        "DELETE" -> Color(0xFFF44336)   // Red
        "PATCH" -> Color(0xFF9C27B0)    // Purple
        "HEAD" -> Color(0xFF607D8B)     // Blue Grey
        else -> Color(0xFF795548)       // Brown
    }
}

// Helper function to get duration colors
private fun getDurationColor(duration: Long): Color {
    return when {
        duration < 200 -> Color(0xFF4CAF50)   // Fast - Green
        duration < 1000 -> Color(0xFFFF9800)  // Medium - Orange
        else -> Color(0xFFF44336)              // Slow - Red
    }
}
