package com.mialarma.app.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mialarma.app.MiAlarmaApplication
import com.mialarma.app.ui.screens.AlarmEditScreen
import com.mialarma.app.ui.screens.AlarmListScreen
import com.mialarma.app.ui.screens.PermissionsScreen
import com.mialarma.app.ui.theme.MiAlarmaTheme
import com.mialarma.app.viewmodel.AlarmViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AlarmViewModel by viewModels {
        AlarmViewModel.factory((application as MiAlarmaApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MiAlarmaTheme {
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* el resultado se refleja al volver a PermissionsScreen */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "list") {
                    composable("list") {
                        AlarmListScreen(
                            viewModel = viewModel,
                            onAddAlarm = { navController.navigate("edit/-1") },
                            onEditAlarm = { id -> navController.navigate("edit/$id") },
                            onOpenSettings = { navController.navigate("permissions") }
                        )
                    }
                    composable(
                        route = "edit/{alarmId}",
                        arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: -1L
                        AlarmEditScreen(
                            alarmId = alarmId,
                            viewModel = viewModel,
                            onDone = { navController.popBackStack() }
                        )
                    }
                    composable("permissions") {
                        PermissionsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
