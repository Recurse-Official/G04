package com.example.dynodroid

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashBoard(
    appItemStates: List<AppItemState>,
    viewModel: DashBoardViewModel,
    scanApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    var selectedAppIndex by remember { mutableIntStateOf(-1) }
    var isSearchActive by remember { mutableStateOf(false) }
    val uploadState by viewModel.uploadState.collectAsState()

    BackHandler(enabled = selectedAppIndex != -1 || isSearchActive) {
        when {
            uploadState is UploadState.Success -> {
                viewModel.resetUploadState()
            }
            selectedAppIndex != -1 -> {
                selectedAppIndex = -1
            }
            isSearchActive -> {
                isSearchActive = false
                searchText = ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            selectedAppIndex != -1 -> "App Details"
                            isSearchActive -> "Search Results"
                            else -> "DynoDroid"
                        },
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    when {
                        selectedAppIndex != -1 || uploadState is UploadState.Success -> {
                            IconButton(onClick = {
                                selectedAppIndex = -1
                                viewModel.resetUploadState()
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                        isSearchActive -> {
                            IconButton(onClick = {
                                isSearchActive = false
                                searchText = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search")
                            }
                        }
                    }
                },
                actions = {
                    if (selectedAppIndex == -1 && uploadState is UploadState.Idle) {
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Toggle Search"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Animated Search Bar
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                    exit = shrinkVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .graphicsLayer {
                                alpha = if (isSearchActive) 1f else 0f
                            }
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        placeholder = {
                            Text(
                                "Search apps...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Filtered Apps
                val filteredApps = appItemStates.filter {
                    it.name.contains(searchText, ignoreCase = true)
                }

                if (selectedAppIndex != -1 || uploadState is UploadState.Success) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // App Item (always visible in details view)
                        AppItem(
                            appItemState = appItemStates[selectedAppIndex],
                            isSelected = true,
                            onSelect = {}
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Conditional rendering based on upload state
                        when (val state = uploadState) {
                            is UploadState.Idle -> {
                                Button(
                                    onClick = { scanApp(appItemStates[selectedAppIndex].packageName) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Scan App", fontWeight = FontWeight.Bold)
                                }
                            }
                            is UploadState.InProgress -> {
                                // Existing progress indicator
                                CircularProgressIndicator(
                                    modifier = Modifier.size(50.dp)
                                )
                                Text(
                                    text = state.message,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            is UploadState.DynamicAnalysis -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(50.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = state.stage,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            is UploadState.Success -> {
                                // Terminal-style results
                                TerminalAnalysisResult(
                                    analysisResult = state.analysisResults
                                        ?: AnalysisResult(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                            is UploadState.Failed -> {
                                val errorMessage = (uploadState as UploadState.Failed).message
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(8.dp),

                                    ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(24.dp)
                                            .fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Warning,
                                            contentDescription = "Error",
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = errorMessage,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { viewModel.resetUploadState() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            )
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        contentPadding = PaddingValues(16.dp),
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredApps) {
                            appItemState -> AppItem(
                                appItemState = appItemState,
                                isSelected = false,
                                onSelect = {
                                    selectedAppIndex = appItemStates.indexOf(appItemState)
                                    isSearchActive = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}