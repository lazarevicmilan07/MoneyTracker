package com.expensetracker.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.data.preferences.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onShowPremium: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userPreferences by viewModel.userPreferences.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportToCsv(context, it) }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { viewModel.exportToPdf(context, it) }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.backup(context, it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restore(context, it) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ExportSuccess -> {
                    Toast.makeText(context, "${event.type} exported successfully", Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.BackupSuccess -> {
                    Toast.makeText(context, "Backup created successfully", Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.RestoreSuccess -> {
                    Toast.makeText(context, "Data restored successfully", Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.ShowPremiumRequired -> {
                    onShowPremium()
                }
                is SettingsEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Premium Banner
            if (!userPreferences.isPremium) {
                item {
                    PremiumBanner(onClick = onShowPremium)
                }
            }

            // Appearance Section
            item {
                SettingsSectionHeader("Appearance")
            }

            item {
                SettingsSwitch(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme",
                    checked = userPreferences.isDarkMode,
                    onCheckedChange = viewModel::setDarkMode
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.AttachMoney,
                    title = "Currency",
                    subtitle = userPreferences.currency,
                    onClick = { viewModel.showCurrencyPicker() }
                )
            }

            // Data Section
            item {
                SettingsSectionHeader("Data")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.TableChart,
                    title = "Export to CSV",
                    subtitle = if (userPreferences.isPremium) "Export all transactions" else "Premium feature",
                    onClick = { csvExportLauncher.launch("expenses_${System.currentTimeMillis()}.csv") },
                    isPremium = !userPreferences.isPremium
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.PictureAsPdf,
                    title = "Export to PDF",
                    subtitle = if (userPreferences.isPremium) "Generate expense report" else "Premium feature",
                    onClick = { pdfExportLauncher.launch("expense_report_${System.currentTimeMillis()}.pdf") },
                    isPremium = !userPreferences.isPremium
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "Backup",
                    subtitle = if (userPreferences.isPremium) "Create data backup" else "Premium feature",
                    onClick = { backupLauncher.launch("expense_backup_${System.currentTimeMillis()}.json") },
                    isPremium = !userPreferences.isPremium
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = "Restore",
                    subtitle = if (userPreferences.isPremium) "Restore from backup" else "Premium feature",
                    onClick = { restoreLauncher.launch(arrayOf("application/json")) },
                    isPremium = !userPreferences.isPremium
                )
            }

            // About Section
            item {
                SettingsSectionHeader("About")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.0",
                    onClick = {}
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Currency Picker Dialog
    if (uiState.showCurrencyPicker) {
        CurrencyPickerDialog(
            currentCurrency = userPreferences.currency,
            onCurrencySelected = { currency ->
                viewModel.setCurrency(currency)
                viewModel.hideCurrencyPicker()
            },
            onDismiss = { viewModel.hideCurrencyPicker() }
        )
    }

    // Loading Overlay
    if (uiState.isExporting || uiState.isBackingUp || uiState.isRestoring) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        text = when {
                            uiState.isExporting -> "Exporting..."
                            uiState.isBackingUp -> "Creating backup..."
                            else -> "Restoring..."
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upgrade to Premium",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Unlock all features with a one-time purchase",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isPremium: Boolean = false
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        trailingContent = {
            if (isPremium) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Premium",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun CurrencyPickerDialog(
    currentCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val currencies = listOf(
        "USD" to "US Dollar",
        "EUR" to "Euro",
        "GBP" to "British Pound",
        "JPY" to "Japanese Yen",
        "CAD" to "Canadian Dollar",
        "AUD" to "Australian Dollar",
        "CHF" to "Swiss Franc",
        "CNY" to "Chinese Yuan",
        "INR" to "Indian Rupee",
        "MXN" to "Mexican Peso",
        "BRL" to "Brazilian Real",
        "KRW" to "South Korean Won"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Currency") },
        text = {
            LazyColumn {
                items(currencies) { (code, name) ->
                    ListItem(
                        headlineContent = { Text(code) },
                        supportingContent = { Text(name) },
                        trailingContent = {
                            if (code == currentCurrency) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.clickable { onCurrencySelected(code) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
