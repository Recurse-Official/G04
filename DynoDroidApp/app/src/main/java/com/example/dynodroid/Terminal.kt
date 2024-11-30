package com.example.dynodroid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun TerminalAnalysisResult(
    analysisResult: AnalysisResult,
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

            Spacer(modifier = Modifier.height(16.dp))

            TerminalSection(
                title = "Dangerous Permissions",
                items = analysisResult.dangerousPermissions,
                highlightColor = Color.Red
            )

            TerminalSection(
                title = "Potential Threats",
                items = analysisResult.potentialThreats,
                highlightColor = Color(0xFFFF6B6B)
            )

            TerminalSection(
                title = "Network Activity",
                items = analysisResult.networkActivity,
                highlightColor = Color(0xFF4ECDC4)
            )

            TerminalSection(
                title = "Static Analysis",
                items = analysisResult.staticAnalysisFindings,
                highlightColor = Color(0xFFFFA726)
            )
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