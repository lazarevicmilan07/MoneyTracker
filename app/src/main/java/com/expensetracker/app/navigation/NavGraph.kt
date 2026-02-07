package com.expensetracker.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.expensetracker.app.data.preferences.PreferencesManager
import com.expensetracker.app.ui.accounts.AccountsScreen
import com.expensetracker.app.ui.categories.CategoriesScreen
import com.expensetracker.app.ui.dashboard.DashboardScreen
import com.expensetracker.app.ui.dashboard.DashboardViewModel
import com.expensetracker.app.ui.premium.PremiumScreen
import com.expensetracker.app.ui.reports.MonthlyReportsScreen
import com.expensetracker.app.ui.reports.YearlyReportsScreen
import com.expensetracker.app.ui.settings.SettingsScreen
import com.expensetracker.app.ui.transaction.TransactionDetailScreen
import com.expensetracker.app.ui.transaction.TransactionScreen

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object MonthlyReports : Screen("monthly_reports")
    data object YearlyReports : Screen("yearly_reports")
    data object AddTransaction : Screen("add_transaction")
    data object EditTransaction : Screen("edit_transaction/{expenseId}") {
        fun createRoute(expenseId: Long) = "edit_transaction/$expenseId"
    }
    data object TransactionDetail : Screen("transaction_detail/{expenseId}") {
        fun createRoute(expenseId: Long) = "transaction_detail/$expenseId"
    }
    data object Categories : Screen("categories")
    data object Accounts : Screen("accounts")
    data object Settings : Screen("settings")
    data object Premium : Screen("premium")
    data object CopyTransaction : Screen("copy_transaction/{expenseId}/{useToday}") {
        fun createRoute(expenseId: Long, useToday: Boolean) = "copy_transaction/$expenseId/$useToday"
    }
    data object AccountsFromTransaction : Screen("accounts_from_transaction")
    data object CategoriesFromTransaction : Screen("categories_from_transaction")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier,
        enterTransition = { slideInVertically(tween(500)) { it / 4 } + fadeIn(tween(500)) },
        exitTransition = { fadeOut(tween(350)) },
        popEnterTransition = { fadeIn(tween(350)) },
        popExitTransition = { slideOutVertically(tween(400)) { it / 4 } + fadeOut(tween(400)) }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.route)
                },
                onViewTransaction = { expenseId ->
                    navController.navigate(Screen.EditTransaction.createRoute(expenseId))
                }
            )
        }

        composable(Screen.AddTransaction.route) {
            TransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAccounts = { navController.navigate(Screen.AccountsFromTransaction.route) },
                onNavigateToCategories = { navController.navigate(Screen.CategoriesFromTransaction.route) }
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument("expenseId") { type = NavType.LongType }
            )
        ) {
            TransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onCopyTransaction = { expenseId, useToday ->
                    navController.navigate(Screen.CopyTransaction.createRoute(expenseId, useToday))
                },
                onNavigateToAccounts = { navController.navigate(Screen.AccountsFromTransaction.route) },
                onNavigateToCategories = { navController.navigate(Screen.CategoriesFromTransaction.route) }
            )
        }

        composable(
            route = Screen.CopyTransaction.route,
            arguments = listOf(
                navArgument("expenseId") { type = NavType.LongType },
                navArgument("useToday") { type = NavType.BoolType }
            )
        ) {
            TransactionScreen(
                onNavigateBack = {
                    navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                },
                onNavigateToAccounts = { navController.navigate(Screen.AccountsFromTransaction.route) },
                onNavigateToCategories = { navController.navigate(Screen.CategoriesFromTransaction.route) }
            )
        }

        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(
                navArgument("expenseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: -1L
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val currency by dashboardViewModel.currency.collectAsState()
            TransactionDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditTransaction = {
                    navController.navigate(Screen.EditTransaction.createRoute(expenseId))
                },
                currency = currency
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(
                onShowPremium = { navController.navigate(Screen.Premium.route) },
                preferencesManager = preferencesManager
            )
        }

        composable(Screen.Accounts.route) {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val currency by dashboardViewModel.currency.collectAsState()
            AccountsScreen(
                currency = currency,
                preferencesManager = preferencesManager
            )
        }

        composable(Screen.AccountsFromTransaction.route) {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val currency by dashboardViewModel.currency.collectAsState()
            AccountsScreen(
                onNavigateBack = { navController.popBackStack() },
                currency = currency,
                preferencesManager = preferencesManager
            )
        }

        composable(Screen.CategoriesFromTransaction.route) {
            CategoriesScreen(
                onNavigateBack = { navController.popBackStack() },
                onShowPremium = { navController.navigate(Screen.Premium.route) },
                preferencesManager = preferencesManager
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onShowPremium = { navController.navigate(Screen.Premium.route) },
                preferencesManager = preferencesManager
            )
        }

        composable(Screen.Premium.route) {
            PremiumScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MonthlyReports.route) {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val currency by dashboardViewModel.currency.collectAsState()
            MonthlyReportsScreen(currency = currency)
        }

        composable(Screen.YearlyReports.route) {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val currency by dashboardViewModel.currency.collectAsState()
            YearlyReportsScreen(currency = currency)
        }
    }
}
