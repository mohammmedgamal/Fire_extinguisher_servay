package com.powerplant.firesurvey

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.powerplant.firesurvey.ui.screens.DetailScreen
import com.powerplant.firesurvey.ui.screens.EditExtinguisherScreen
import com.powerplant.firesurvey.ui.screens.HomeScreen
import com.powerplant.firesurvey.ui.screens.ScanScreen
import com.powerplant.firesurvey.ui.screens.SurveyScreen
import com.powerplant.firesurvey.ui.theme.FireSurveyTheme

object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val DETAIL = "extinguisher/{code}"
    const val SURVEY = "survey/{code}"
    const val EDIT = "edit?code={code}&isNew={isNew}"

    fun detail(code: String) = "extinguisher/${Uri.encode(code)}"
    fun survey(code: String) = "survey/${Uri.encode(code)}"
    fun edit(code: String, isNew: Boolean) = "edit?code=${Uri.encode(code)}&isNew=$isNew"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FireSurveyTheme {
                val nav = rememberNavController()
                val vm: SurveyViewModel = viewModel()
                val codeArg = navArgument("code") { type = NavType.StringType; defaultValue = "" }

                NavHost(navController = nav, startDestination = Routes.HOME) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            vm = vm,
                            onScan = { nav.navigate(Routes.SCAN) },
                            onOpen = { nav.navigate(Routes.detail(it)) },
                            onAdd = { nav.navigate(Routes.edit("", isNew = true)) },
                        )
                    }
                    composable(Routes.SCAN) {
                        ScanScreen(
                            onBack = { nav.popBackStack() },
                            onCodeScanned = { code ->
                                nav.navigate(Routes.detail(code)) { popUpTo(Routes.SCAN) { inclusive = true } }
                            },
                        )
                    }
                    composable(Routes.DETAIL, arguments = listOf(codeArg)) { entry ->
                        val code = entry.arguments?.getString("code").orEmpty()
                        DetailScreen(
                            vm = vm,
                            code = code,
                            onBack = { nav.popBackStack() },
                            onSurvey = { nav.navigate(Routes.survey(code)) },
                            onEdit = { nav.navigate(Routes.edit(code, isNew = false)) },
                            onRegister = {
                                nav.navigate(Routes.edit(code, isNew = true)) {
                                    popUpTo(Routes.DETAIL) { inclusive = true }
                                }
                            },
                        )
                    }
                    composable(Routes.SURVEY, arguments = listOf(codeArg)) { entry ->
                        SurveyScreen(
                            vm = vm,
                            code = entry.arguments?.getString("code").orEmpty(),
                            onDone = { nav.popBackStack() },
                        )
                    }
                    composable(
                        Routes.EDIT,
                        arguments = listOf(
                            codeArg,
                            navArgument("isNew") { type = NavType.BoolType; defaultValue = true },
                        ),
                    ) { entry ->
                        val isNew = entry.arguments?.getBoolean("isNew") ?: true
                        EditExtinguisherScreen(
                            vm = vm,
                            initialCode = entry.arguments?.getString("code").orEmpty(),
                            isNew = isNew,
                            onBack = { nav.popBackStack() },
                            onSaved = { code ->
                                if (isNew) {
                                    nav.navigate(Routes.detail(code)) { popUpTo(Routes.EDIT) { inclusive = true } }
                                } else {
                                    nav.popBackStack()
                                }
                            },
                            onDeleted = { nav.popBackStack(Routes.HOME, inclusive = false) },
                        )
                    }
                }
            }
        }
    }
}
