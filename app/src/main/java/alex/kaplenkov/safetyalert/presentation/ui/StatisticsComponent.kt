package alex.kaplenkov.safetyalert.presentation.ui

import alex.kaplenkov.safetyalert.domain.model.Violation
import alex.kaplenkov.safetyalert.utils.formatTimestamp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun StatisticsSection(
    violations: List<Violation>,
    violationsByType: Map<String, Int>,
    violationsByWeek: Map<String, Int>,
    mostCommonViolationType: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Статистика нарушений",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )


        SummaryStatisticsCards(
            totalViolations = violations.size,
            lastViolationDate = violations.maxByOrNull { it.timestamp }?.timestamp,
            mostCommonViolationType = mostCommonViolationType
        )

        Spacer(modifier = Modifier.height(24.dp))


        if (violationsByType.isNotEmpty()) {
            Text(
                text = "Распределение по типам нарушений",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                PieChartWithLegend(
                    data = violationsByType,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }


        if (violationsByWeek.isNotEmpty()) {
            Text(
                text = "Динамика нарушений по неделям",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                BarChartWithLabels(
                    data = violationsByWeek,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun SummaryStatisticsCards(
    totalViolations: Int,
    lastViolationDate: String?,
    mostCommonViolationType: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        StatisticCard(
            title = "Всего",
            value = totalViolations.toString(),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary
        )


        StatisticCard(
            title = "Последнее",
            value = lastViolationDate?.let { formatTimestamp(it) } ?: "Н/Д",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.secondary
        )
    }

    Spacer(modifier = Modifier.height(8.dp))


    StatisticCard(
        title = "Частое нарушение",
        value = mostCommonViolationType ?: "Н/Д",
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiary
    )
}

@Composable
fun StatisticCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        modifier = modifier.padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = color,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PieChartWithLegend(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum().toFloat()
    val colors = listOf(
        Color(0xFF4285F4), // Blue
        Color(0xFFEA4335), // Red
        Color(0xFFFBBC05), // Yellow
        Color(0xFF34A853), // Green
        Color(0xFF9C27B0), // Purple
        Color(0xFFFF9800), // Orange
        Color(0xFF00BCD4), // Cyan
        Color(0xFF795548)  // Brown
    )

    val legendItems = data.keys.zip(colors.take(data.size)).toList()

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(180.dp)
                .weight(1.5f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                var startAngle = -90f
                val radius = size.minDimension / 2
                val centerX = size.width / 2
                val centerY = size.height / 2

                data.values.forEachIndexed { index, value ->
                    val sweepAngle = 360f * (value / total)
                    val color = colors[index % colors.size]


                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2)
                    )


                    drawArc(
                        color = Color.White,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        style = Stroke(width = 2f),
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2)
                    )

                    startAngle += sweepAngle
                }
            }
        }


        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            legendItems.forEachIndexed { index, (label, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(12.dp),
                        color = color,
                        shape = RoundedCornerShape(2.dp)
                    ) {}

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "$label (${data[label]})",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun BarChartWithLabels(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val sortedData = data.toList().sortedBy {
        it.first.substringAfter("Week ").substringBefore(",").toIntOrNull() ?: 0
    }.toMap()

    val maxValue = sortedData.values.maxOrNull() ?: 0
    val barColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bottomPadding = 70f
            val barWidth = (size.width - 60f) / sortedData.size
            val verticalScale = (size.height - 80f - (bottomPadding - 40f)) / (maxValue + 1f)


            drawLine(
                color = Color.Gray,
                start = Offset(40f, 20f),
                end = Offset(40f, size.height - bottomPadding),
                strokeWidth = 2f
            )


            drawLine(
                color = Color.Gray,
                start = Offset(40f, size.height - bottomPadding),
                end = Offset(size.width, size.height - bottomPadding),
                strokeWidth = 2f
            )


            drawContext.canvas.nativeCanvas.drawText(
                "Количество нарушений",
                10f,
                10f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 30f
                }
            )

            drawContext.canvas.nativeCanvas.drawText(
                "Неделя",
                size.width / 2 - 40f,
                size.height - 10f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 30f
                }
            )

            val guidelineCount = 5
            for (i in 0..guidelineCount) {
                val y = size.height - bottomPadding - (i * (size.height - 80f - (bottomPadding - 40f)) / guidelineCount)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.7f),
                    start = Offset(40f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )

                val value = ((i * maxValue) / guidelineCount)
                drawContext.canvas.nativeCanvas.drawText(
                    value.toString(),
                    20f,
                    y + 5f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 28f
                    }
                )
            }

            sortedData.entries.forEachIndexed { index, entry ->
                val barHeight = entry.value * verticalScale
                val x = 40f + index * barWidth + 10f

                drawRect(
                    color = barColor,
                    topLeft = Offset(x, size.height - bottomPadding - barHeight),
                    size = Size(barWidth - 20f, barHeight)
                )

                if (entry.value > 0) {
                    drawContext.canvas.nativeCanvas.drawText(
                        entry.value.toString(),
                        x + (barWidth - 20f) / 2 - 8f,
                        size.height - bottomPadding - barHeight - 5f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 28f
                        }
                    )
                }

                val label = entry.key.substringAfter("Week ").substringBefore(",")
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x + (barWidth - 20f) / 2 - 8f,
                    size.height - bottomPadding + 25f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 28f
                    }
                )
            }
        }
    }
}