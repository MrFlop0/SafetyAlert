package alex.kaplenkov.safetyalert.presentation.ui

import alex.kaplenkov.safetyalert.domain.model.Violation
import alex.kaplenkov.safetyalert.presentation.viewmodel.ViolationViewModel
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    navController: NavController,
    viewModel: ViolationViewModel = hiltViewModel(),
    sessionId: String = ""
) {
    val violations by viewModel.getViolationsForSession(sessionId)
        .collectAsState(initial = emptyList())

    val totalViolationsCount by viewModel.totalViolationsCount.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.endSession()
        Log.d("ReportScreen", "Received session ID: $sessionId")
    }

    val onNavigateHome = {
        navController.navigate(MainScreen)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отчет по нарушениям безопасности") },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.Default.Home, contentDescription = "На главный экран")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Column {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Статистика нарушений",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нарушений в текущей сессии: ${violations.size}",
                            style = MaterialTheme.typography.bodyLarge
                        )


                        if (totalViolationsCount > violations.size) {
                            Text(
                                text = "Всего нарушений в базе: $totalViolationsCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }

                if (violations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "В текущей сессии нарушений не обнаружено",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(violations) { violation ->
                            ViolationCard(
                                violation = violation,
                                onDeleteClick = {
                                    viewModel.deleteViolation(violation.id)
                                },
                                onCardClick = {
                                    navController.navigate(ViolationDetailScreen(violation.id))
                                }
                            )
                        }
                    }
                }
            }


            Button(
                onClick = onNavigateHome,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "На главный экран",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("На главный экран")
            }
        }
    }
}

@Composable
fun ViolationCard(
    violation: Violation,
    onDeleteClick: () -> Unit,
    onCardClick: (Long) -> Unit = {}
) {

    val cardColor = when {
        violation.type.contains("Helmet", ignoreCase = true) ->
            Color(0xFFF5F5DC)
        violation.type.contains("Stair") || violation.type.contains("Handrail", ignoreCase = true) ->
            Color(0xFFE0F7FA)
        else ->
            Color(0xFFF5F5F5)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick(violation.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = getViolationTitle(violation.type),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))


            val severityColor = when {
                violation.confidence > 0.9 -> Color(0xFFE57373) // Red
                violation.confidence > 0.7 -> Color(0xFFFFB74D) // Orange
                else -> Color(0xFFAED581) // Green
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Severity:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(severityColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${(violation.confidence * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val formattedTime = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(violation.timestamp)
                val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                outputFormat.format(date!!)
            } catch (e: Exception) {
                violation.timestamp
            }

            Text(
                text = "Detected: $formattedTime",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            if (violation.location != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Location: ${violation.location}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))


            violation.description?.let {
                Text(
                    text = getDescriptionForViolationType(violation.type, it),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }


            violation.imageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Violation evidence",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete violation",
                        tint = Color.Red
                    )
                }
            }
        }
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
            "⚠️ $description\n\nНарушение правил безопасного передвижения по лестнице: пешеход не держится за перила."
        else -> description
    }
}

@Serializable
data class ReportScreen(val sessionId: String)