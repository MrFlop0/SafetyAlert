package alex.kaplenkov.safetyalert.presentation.ui

import alex.kaplenkov.safetyalert.presentation.viewmodel.ViolationViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllViolationsScreen(
    navController: NavController,
    viewModel: ViolationViewModel = hiltViewModel()
) {
    val allViolations by viewModel.allViolations.collectAsState()
    val violationsByType by viewModel.violationsByType.collectAsState()
    val violationsByWeek by viewModel.violationsByWeek.collectAsState()
    val mostCommonViolationType by viewModel.mostCommonViolationType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Архив всех нарушений") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                StatisticsSection(
                    violations = allViolations,
                    violationsByType = violationsByType,
                    violationsByWeek = violationsByWeek,
                    mostCommonViolationType = mostCommonViolationType
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Text(
                    text = "Список всех нарушений",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (allViolations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нарушений не обнаружено",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(allViolations) { violation ->
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
}


@Serializable
object AllViolationsScreen