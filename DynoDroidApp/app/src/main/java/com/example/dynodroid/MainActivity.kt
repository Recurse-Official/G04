package com.example.dynodroid

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.dynodroid.ui.theme.DynoDroidTheme

class MainActivity : ComponentActivity() {
    private lateinit var appDetails: List<AppDetail>

    // Initialize ViewModel
    private val dashBoardViewModel: DashBoardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Retrieve installed apps when the activity is created
        appDetails = retrieveInstalledApps()

        setContent {
            DynoDroidTheme {
                Scaffold { paddingValues ->
                    DashBoard(
                        appItemStates = appDetails.map {
                            AppItemState(
                                name = it.appName,
                                packageName = it.packageName,
                                icon = it.icon
                            )
                        },
                        viewModel = dashBoardViewModel,
                        scanApp = { packageName ->
                            dashBoardViewModel.scanAndSendApk(
                                appName = appDetails.firstOrNull { it.packageName == packageName }?.appName.orEmpty(),
                                packageName = packageName,
                                context = this
                            )
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    /**
     * Retrieves a list of non-system applications installed on the device.
     *
     * @return A list of AppDetail objects representing the installed apps.
     */
    private fun retrieveInstalledApps(): List<AppDetail> {
        val packageManager = packageManager

        // Get all installed applications and filter out system apps
        val installedApps: List<ApplicationInfo> = packageManager.getInstalledApplications(
            PackageManager.GET_META_DATA or PackageManager.GET_ACTIVITIES
        ).filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }

        // Convert ApplicationInfo objects to AppDetail objects
        return installedApps.map { appInfo ->
            AppDetail(
                appName = appInfo.loadLabel(packageManager).toString(),
                packageName = appInfo.packageName,
                icon = appInfo.loadIcon(packageManager).toBitmap().asImageBitmap(),
                sourceDir = appInfo.sourceDir,
                splitSourceDirs = appInfo.splitSourceDirs
            )
        }.sortedBy { it.appName.length }
    }
}