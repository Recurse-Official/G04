package com.example.dynodroid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TerminalAnalysisResult(
    analysisResult: AnalysisStatusResponse,
    staticAnalysisResult: StaticAnalysisResult?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "DynoDroid Security Analysis",
                color = Color.Green,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace
            )

            // Static Analysis Section
            staticAnalysisResult?.let { staticResult ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Malware Prediction: ${staticResult.malwarePrediction}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
                Text(
                    text = ">> Static Analysis",
                    color = Color(0xFF32CD32), // Lime green
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center 
                )
                TerminalSection(
                    title = "App Components",
                    items = listOf(
                        "Permissions: ${staticResult.permissionCount}",
                        "Activities: ${staticResult.activityCount}",
                        "Services: ${staticResult.serviceCount}",
                        "Receivers: ${staticResult.receiverCount}",
                        "Providers: ${staticResult.providerCount}"
                    ),
                    highlightColor = Color(0xFF1E90FF) // Dodger blue
                )
            }

            Text(
                text = ">> Dynamic Analysis",
                color = Color(0xFF32CD32), // Lime green
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Top Syscalls Section
            Text(
                text = ">> Top Syscalls",
                color = Color(0xFF6A5ACD), // Slate blue
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            analysisResult.previousResults?.topSyscalls?.forEachIndexed { index, syscall ->
                Text(
                    text = "${index + 1}. ${syscall[0]} (${syscall[1]} calls)",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Top RedZone Syscalls Section
            Text(
                text = ">> Top RedZone Syscalls",
                color = Color(0xFFFF4500), // Orange-red
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            analysisResult.previousResults?.topRedZonesyscalls?.forEachIndexed { index, syscall ->
                Text(
                    text = "${index + 1}. ${syscall[0]} (${syscall[1]} calls)",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TerminalSection(
    title: String,
    items: List<String>,
    highlightColor: Color
) {
    Text(
        text = ">> $title",
        color = highlightColor,
        style = MaterialTheme.typography.titleMedium,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    items.forEachIndexed { index, item ->
        Text(
            text = "${index + 1}. $item",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
}