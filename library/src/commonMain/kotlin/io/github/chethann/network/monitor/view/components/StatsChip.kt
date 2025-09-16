package io.github.chethann.network.monitor.view.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.chethann.network.monitor.view.theme.extendedColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatsChip(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(horizontal = 4.dp),
        backgroundColor = color.copy(alpha = if (MaterialTheme.colors.isLight) 0.12f else 0.22f),
        elevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                color = if (color == MaterialTheme.colors.onSurface.copy(alpha = 0.5f)) MaterialTheme.colors.onSurface.copy(alpha = 0.75f) else color,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = label,
                color = if (color == MaterialTheme.colors.onSurface.copy(alpha = 0.5f)) MaterialTheme.colors.onSurface.copy(alpha = 0.6f) else color,
                fontSize = 10.sp
            )
        }
    }
}
