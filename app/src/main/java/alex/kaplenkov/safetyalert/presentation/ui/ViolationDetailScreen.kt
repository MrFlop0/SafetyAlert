package alex.kaplenkov.safetyalert.presentation.ui

import alex.kaplenkov.safetyalert.presentation.viewmodel.ViolationViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViolationDetailScreen(
    navController: NavController,
    violationId: Long,
    viewModel: ViolationViewModel = hiltViewModel()
) {
    val violation by viewModel.getViolationById(violationId).collectAsState(initial = null)
    val scrollState = rememberScrollState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали нарушения") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить нарушение",
                            tint = Color.Red
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        violation?.let { violationData ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    violationData.imageUri?.let { uri ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Изображение нарушения",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .background(Color(0x99000000))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = getViolationTitle(violationData.type),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(getSeverityColor(violationData.confidence))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getViolationTitle(violationData.type),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Уровень серьезности:",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(getSeverityColor(violationData.confidence))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${(violationData.confidence * 100).toInt()}%",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            val formattedTime = try {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                val date = inputFormat.parse(violationData.timestamp)
                                val outputFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
                                outputFormat.format(date!!)
                            } catch (e: Exception) {
                                violationData.timestamp
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Время обнаружения",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = formattedTime,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }

                            violationData.location?.let { location ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Местоположение",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = location,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))


                    Text(
                        text = "Описание нарушения",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            violationData.description?.let { description ->
                                Text(
                                    text = getDescriptionForViolationType(violationData.type, description),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            } ?: Text(
                                text = "Описание отсутствует",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )


                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Рекомендации:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = getRecommendationForViolationType(violationData.type),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Назад")
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = { /* Future functionality: Add notes or edit violation */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Добавить заметку")
                        }
                    }
                }

                if (showDeleteConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmation = false },
                        title = { Text("Подтверждение удаления") },
                        text = { Text("Вы уверены, что хотите удалить это нарушение?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.deleteViolation(violationData.id)
                                    showDeleteConfirmation = false
                                    navController.popBackStack()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                )
                            ) {
                                Text("Удалить")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                                Text("Отмена")
                            }
                        }
                    )
                }
            }
        } ?: run {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Нарушение не найдено",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun getSeverityColor(confidence: Float): Color {
    return when {
        confidence > 0.9 -> Color(0xFFE57373) // Red
        confidence > 0.7 -> Color(0xFFFFB74D) // Orange
        else -> Color(0xFFAED581) // Green
    }
}

private fun getViolationTitle(type: String): String {
    return when {
        type.contains("Helmet", ignoreCase = true) -> "Отсутствие защитной каски"
        type.contains("Stair") || type.contains("Handrail", ignoreCase = true) -> "Передвижение без опоры на перила"
        else -> type
    }
}

private fun getDescriptionForViolationType(type: String, description: String): String {
    return when {
        type.contains("Helmet", ignoreCase = true) ->
            "⚠️ $description\n\nНарушение требований безопасности: рабочий находится в зоне, требующей ношения защитной каски."
        type.contains("Stair") || type.contains("Handrail", ignoreCase = true) ->
            "⚠️ $description\n\nНарушение правил безопасного передвижения по лестнице: рабочий не держится за перила."
        else -> description
    }
}

private fun getRecommendationForViolationType(type: String): String {
    return when {
        type.contains("Helmet", ignoreCase = true) ->
            "1. Немедленно остановить работу сотрудника\n" +
                    "2. Проинструктировать о необходимости ношения защитной каски в рабочей зоне\n" +
                    "3. Проверить наличие и состояние средств индивидуальной защиты\n" +
                    "4. Провести внеплановый инструктаж по технике безопасности"

        type.contains("Stair") || type.contains("Handrail", ignoreCase = true) ->
            "1. Обратить внимание сотрудника на правила передвижения по лестнице\n" +
                    "2. Проверить состояние перил и надежность их крепления\n" +
                    "3. Разместить информационные таблички о необходимости держаться за перила\n" +
                    "4. Включить данный случай в программу регулярных инструктажей"

        else -> "Свяжитесь с руководителем безопасности для получения рекомендаций по данному типу нарушения."
    }
}

@Serializable
data class ViolationDetailScreen(val violationId: Long)