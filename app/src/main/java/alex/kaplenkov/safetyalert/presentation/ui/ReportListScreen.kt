package alex.kaplenkov.safetyalert.presentation.ui

import alex.kaplenkov.safetyalert.domain.model.DetectionReport
import alex.kaplenkov.safetyalert.presentation.viewmodel.ReportListEvent
import alex.kaplenkov.safetyalert.presentation.viewmodel.ReportListViewModel
import alex.kaplenkov.safetyalert.utils.formatDate
import alex.kaplenkov.safetyalert.utils.formatDuration
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportListScreen(
    navController: NavController,
    viewModel: ReportListViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    // Navigate to report detail when a report is selected
    LaunchedEffect(state.selectedReportId) {
        state.selectedReportId?.let { reportId ->
            navController.navigate("report_detail/$reportId")
            viewModel.onEvent(ReportListEvent.SelectReport(""))  // Reset selection
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety Alert Reports") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.reports.isEmpty()) {
                Text(
                    text = "No reports found",
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 18.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.reports) { report ->
                        ReportItem(
                            report = report,
                            onReportClick = {
                                viewModel.onEvent(ReportListEvent.SelectReport(report.id))
                            },
                            onDeleteClick = {
                                viewModel.onEvent(ReportListEvent.ShowDeleteDialog(report))
                            }
                        )
                    }
                }
            }

            // Show error message if any
            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(error)
                }
            }
        }
    }

    // Delete confirmation dialog
    state.reportToDelete?.let { report ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ReportListEvent.DismissDeleteDialog) },
            title = { Text("Delete Report") },
            text = { Text("Are you sure you want to delete this report?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(ReportListEvent.DeleteReport) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.onEvent(ReportListEvent.DismissDeleteDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReportItem(
    report: DetectionReport,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onReportClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Report: ${formatDate(report.startTime)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Duration: ${formatDuration(report.duration)}",
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Total Detections: ${report.totalDetections}",
                    fontSize = 14.sp
                )

                if (report.totalPeopleWithoutHelmets > 0) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "People without helmets: ${report.totalPeopleWithoutHelmets}",
                        fontSize = 14.sp,
                        color = Color.Red
                    )
                }
            }
        }
    }
}

@Serializable
object ReportListScreen